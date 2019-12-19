package com.hpe.amce.translation

import com.codahale.metrics.MetricRegistry
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import spock.lang.Shared
import spock.lang.Specification

class ResilientBatchTranslatorTest extends Specification {

    class RawEvent {
        int num

        @Override
        String toString() {
            "r$num"
        }
    }

    class TranslatedEvent {
        int num

        @Override
        String toString() {
            "t$num"
        }
    }

    class Context {
        String param

        @Override
        String toString() {
            "c$param"
        }
    }

    ResilientBatchTranslator<Context> instance

    @Shared
    private ListAppender listAppender = LoggerContext.getContext(false).
            getRootLogger().appenders.get("LIST") as ListAppender

    void setup() {
        instance = new ResilientBatchTranslator()
        instance.name = getClass().name
        instance.traceLevel = Level.INFO
        instance.metricRegistry = new MetricRegistry()
        listAppender.clear()
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

    def "metrics recorded"() {
        given:
        instance.processingStages = [
                dropEven  : { RawEvent event, Object[] params ->
                    event.num % 2 == 0 ? [] : [event]
                },
                translator: { RawEvent event, Object[] params ->
                    [new TranslatedEvent(num: event.num)]
                },
        ]
        instance.lookupLoggers()
        int batchCount = 3
        int batchSize = 4
        long expectedOutBatchSize = (batchSize / 2).longValueExact()
        when:
        (1..batchCount).each {
            instance.translateBatch((1..batchSize).collect { new RawEvent(num: it) }, new Context(param: "ems"))
        }
        then:
        instance.metricRegistry.histogram(getClass().name + ".in.batch_size").snapshot.max == batchSize
        instance.metricRegistry.histogram(getClass().name + ".out.batch_size").snapshot.max == expectedOutBatchSize
        instance.metricRegistry.meter(getClass().name + ".batches").count == batchCount
        instance.metricRegistry.meter(getClass().name + ".in.events").count == batchCount * batchSize
        instance.metricRegistry.meter(getClass().name + ".out.events").count == batchCount * expectedOutBatchSize
        instance.metricRegistry.timer(getClass().name + ".translate_batch").count == batchCount
        instance.metricRegistry.timer(getClass().name + ".one.dropEven").count == batchCount * batchSize
        instance.metricRegistry.timer(getClass().name + ".one.translator").count == batchCount * expectedOutBatchSize
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

    def "data is traced"() {
        given:
        instance.processingStages = [
                stage1: { RawEvent raw, Context context -> [new RawEvent(num: raw.num + 1)] },
                stage2: { RawEvent raw, Context context -> [new RawEvent(num: raw.num + 1)] },
                stage3: { RawEvent raw, Context context -> [new TranslatedEvent(num: raw.num + 1)] },
        ]
        instance.lookupLoggers()
        when:
        instance.translateBatch([
                new RawEvent(num: 1000),
                new RawEvent(num: 2000),
                new RawEvent(num: 3000)
        ],
                new Context(param: "ems"))
        List<LogEvent> logs = listAppender.events.findAll { it.loggerName.startsWith(instance.name) }
        then: "uses specified log level"
        logs.every { it.level == instance.traceLevel }
        then: "context is always logged"
        logs.every { it.message.formattedMessage.contains('ems') }
        then: "input is logged"
        logs.find { it.message.formattedMessage.contains('1000') }.loggerName.endsWith('input')
        then: "intermediate stages are logged"
        logs.find { it.message.formattedMessage.contains('1001') }.loggerName.endsWith('stage1')
        logs.find { it.message.formattedMessage.contains('1002') }.loggerName.endsWith('stage2')
        then: "output is logged"
        logs.find { it.message.formattedMessage.contains('1003') }.loggerName.endsWith('stage3')
        then: "elements separated by new line"
        logs.find { it.message.formattedMessage.contains('1000') }.message.formattedMessage
                .split(System.lineSeparator())
                .findAll { line -> ['1000', '2000', '3000'].any { line.contains(it) } }
                .size() == 3
    }

}
