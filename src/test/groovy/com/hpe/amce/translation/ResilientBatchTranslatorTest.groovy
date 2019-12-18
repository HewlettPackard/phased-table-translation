package com.hpe.amce.translation

import com.codahale.metrics.Histogram
import com.codahale.metrics.MetricRegistry
import org.apache.logging.log4j.Level
import spock.lang.Specification

class ResilientBatchTranslatorTest extends Specification {

    class RawEvent {
        int num
    }

    class TranslatedEvent {
        int num
    }

    class Context {
        String param
    }

    ResilientBatchTranslator<Context> instance

    void setup() {
        instance = new ResilientBatchTranslator()
        instance.name = getClass().name
        instance.traceLevel = Level.INFO
        instance.metricRegistry = new MetricRegistry()
    }

    def "simple case with context"() {
        given:
        instance.processingStages = [
                pre  : { RawEvent event, Context context ->
                    assert event && event instanceof RawEvent
                    assert context?.param == "ems"
                    [event]
                },
                trans: { RawEvent raw, Context context ->
                    assert raw && raw instanceof RawEvent
                    assert context?.param == "ems"
                    [new TranslatedEvent()]
                },
                post : { TranslatedEvent event, Context context ->
                    assert event && event instanceof TranslatedEvent
                    assert context?.param == "ems"
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List translated = instance.translateBatch([new RawEvent()], new Context(param: "ems"))
        then:
        assert translated
        assert translated.size() == 1
        assert translated[0] && translated[0] instanceof TranslatedEvent
    }

    def "context is optional"() {
        given:
        instance.processingStages = [
                pre  : { RawEvent event, Context context ->
                    assert event && event instanceof RawEvent
                    assert !context
                    [event]
                },
                trans: { RawEvent raw, Context context ->
                    assert raw && raw instanceof RawEvent
                    assert !context
                    [new TranslatedEvent()]
                },
                post : { TranslatedEvent event, Context context ->
                    assert event && event instanceof TranslatedEvent
                    assert !context
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List translated = instance.translateBatch([new RawEvent()])
        then:
        assert translated
        assert translated.size() == 1
        assert translated[0] && translated[0] instanceof TranslatedEvent
    }

    def "errors on elements are ignored"() {
        given:
        instance.processingStages = [
                pre  : { RawEvent event, Object[] params ->
                    if (event.num == 1) {
                        throw new NullPointerException("test exception that should be ignored")
                    }
                    [event]
                },
                trans: { RawEvent event, Object[] params ->
                    if (event.num == 2) {
                        throw new NullPointerException("test exception that should be ignored")
                    }
                    [new TranslatedEvent(num: event.num)]
                },
                post : { TranslatedEvent event, Object[] params ->
                    if (event.num == 3) {
                        throw new NullPointerException("test exception that should be ignored")
                    }
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List<TranslatedEvent> translated = instance.translateBatch([
                new RawEvent(num: 1),
                new RawEvent(num: 2),
                new RawEvent(num: 3),
                new RawEvent(num: 4),
        ], new Context(param: "ems"))*.asType(TranslatedEvent)
        then:
        assert translated
        assert translated.size() == 1
        assert translated[0].num == 4
    }

    def "nulls are ignored"() {
        given:
        instance.processingStages = [
                pre  : { RawEvent event, Object[] params ->
                    assert event && event instanceof RawEvent
                    null
                },
                trans: { RawEvent raw, Object[] params ->
                    assert raw && raw instanceof RawEvent
                    [null]
                },
                post : { TranslatedEvent event, Object[] params ->
                    assert event && event instanceof TranslatedEvent
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List translated = instance.translateBatch([new RawEvent(), null], new Context(param: "ems"))
        then:
        assert translated != null
        assert translated.empty
    }

    def "empty lists as result of closure means filtering"() {
        given:
        instance.processingStages = [
                pre  : { RawEvent event, Object[] params ->
                    if (event.num == 1) {
                        return []
                    }
                    [event]
                },
                trans: { RawEvent event, Object[] params ->
                    if (event.num == 2) {
                        return []
                    }
                    [new TranslatedEvent(num: event.num)]
                },
                post : { TranslatedEvent event, Object[] params ->
                    if (event.num == 3) {
                        return []
                    }
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List<TranslatedEvent> translated = instance.translateBatch([
                new RawEvent(num: 1),
                new RawEvent(num: 2),
                new RawEvent(num: 3),
                new RawEvent(num: 4),
        ], new Context(param: "ems"))*.asType(TranslatedEvent)
        then:
        assert translated
        assert translated.size() == 1
        assert translated[0] && translated[0] instanceof TranslatedEvent
        assert translated[0].num == 4
    }

    def "translator can inject new elements"() {
        given:
        instance.processingStages = [
                pre  : { RawEvent event, Object[] params ->
                    if (event.num == 1) {
                        return [event, new RawEvent(num: 2)]
                    }
                    [event]
                },
                trans: { RawEvent event, Object[] params ->
                    if (event.num == 3) {
                        return [new TranslatedEvent(num: 3), new TranslatedEvent(num: 4)]
                    }
                    [new TranslatedEvent(num: event.num)]
                },
                post : { TranslatedEvent event, Object[] params ->
                    if (event.num == 5) {
                        return [event, new TranslatedEvent(num: 6)]
                    }
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List<TranslatedEvent> translated = instance.translateBatch([
                new RawEvent(num: 1),
                new RawEvent(num: 3),
                new RawEvent(num: 5),
        ])*.asType(TranslatedEvent)
        then:
        assert translated
        (0..5).each {
            assert translated[it].num == it + 1
        }
    }

    def "batch sizes recorded in metrics"() {
        given:
        instance.processingStages = [
                trans: { RawEvent event, Object[] params ->
                    [new TranslatedEvent(num: event.num)]
                },
        ]
        instance.lookupLoggers()
        when:
        Histogram histogram = instance.metricRegistry.histogram(getClass().name + ".in.batch_size")
        instance.translateBatch([
                new RawEvent(num: 1),
                new RawEvent(num: 2),
                new RawEvent(num: 3),
                new RawEvent(num: 4),
        ], new Context(param: "ems"))
        then:
        histogram.count == 1
        histogram.snapshot.min == 4
        histogram.snapshot.max == 4
    }

    def "all types of errors are caught"() {
        given:
        instance.processingStages = [
                npe        : { RawEvent event, Object[] params ->
                    if (event.num == 1) {
                        String npe = null
                        npe.trim()
                    }
                    [event]
                },
                powerAssert: { RawEvent event, Object[] params ->
                    if (event.num == 2) {
                        assert false
                    }
                    [new TranslatedEvent(num: event.num)]
                },
                io         : { TranslatedEvent event, Object[] params ->
                    if (event.num == 3) {
                        new File("/ d o e s n o t e x i s t _ @ # ! \$ , :").getText()
                    }
                    [event]
                },
        ]
        instance.lookupLoggers()
        when:
        List<TranslatedEvent> translated = instance.translateBatch([
                new RawEvent(num: 1),
                new RawEvent(num: 2),
                new RawEvent(num: 3),
                new RawEvent(num: 4),
        ], new Context(param: "ems"))*.asType(TranslatedEvent)
        then:
        assert translated
        assert translated.size() == 1
        assert translated[0] && translated[0] instanceof TranslatedEvent
        assert translated[0].num == 4
    }

}
