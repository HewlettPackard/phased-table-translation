package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import spock.lang.Specification

import java.math.RoundingMode

/**
 * Test that new API can be used to achieve alike functionality as was done by legacy code.
 */
// TODO: make an example of all possible functionality?
class OnParWithLegacyTest extends Specification {

    class RawEvent {
        int num
    }

    class TranslatedEvent {
        int num
    }

    class Context {
        String param
    }

    DecorableStagedBatchTranslator<RawEvent, TranslatedEvent, Context> instance

    MetricRegistry metricRegistry

    void setup() {
        instance = new DecorableStagedBatchTranslator<>()
        metricRegistry = new MetricRegistry()
        instance.aroundBatch = new BatchMeteringDecorator<>(
                instance.aroundBatch, metricRegistry, getClass().name
        ).tap { decorator ->
            // Force legacy metric names
            decorator.incomingBatchCountMetricName = { "${decorator.metricsBaseName}.batches" }
        }
        instance.aroundElement =
                new ElementMeteringDecorator<>(
                        new ElementErrorSuppressorDecorator<>(
                                new ElementErrorMeteringDecorator<>(
                                        new StageCaller<>(),
                                        metricRegistry,
                                        getClass().name + '.')
                        ),
                        metricRegistry,
                        getClass().name
                ).tap {
                    // Force legacy metric names
                    it.timerName = { String stageName -> "${it.metricsBaseName}.one.$stageName" }
                }
        instance.aroundStage = new StageMeteringDecorator<>(
                instance.aroundStage, metricRegistry, getClass().name + '.')
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
        when:
        List translated = instance.translateBatch([new RawEvent()], null)
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
        when:
        List<TranslatedEvent> translated = instance.translateBatch([
                new RawEvent(num: 1),
                new RawEvent(num: 3),
                new RawEvent(num: 5),
        ], null)*.asType(TranslatedEvent)
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
        int batchCount = 3
        int batchSize = 4
        long expectedOutBatchSize = (batchSize / 2).longValueExact()
        when:
        (1..batchCount).each {
            instance.translateBatch((1..batchSize).collect { new RawEvent(num: it) }, new Context(param: "ems"))
        }
        then:
        metricRegistry.histogram(getClass().name + ".in.batch_size").snapshot.max == batchSize
        metricRegistry.histogram(getClass().name + ".out.batch_size").snapshot.max == expectedOutBatchSize
        metricRegistry.meter(getClass().name + ".batches").count == batchCount
        metricRegistry.meter(getClass().name + ".in.events").count == batchCount * batchSize
        metricRegistry.meter(getClass().name + ".out.events").count == batchCount * expectedOutBatchSize
        metricRegistry.timer(getClass().name + ".translate_batch").count == batchCount
        metricRegistry.timer(getClass().name + ".one.dropEven").count == batchCount * batchSize
        metricRegistry.timer(getClass().name + ".one.translator").count == batchCount * expectedOutBatchSize
        metricRegistry.histogram(getClass().name + ".dropEven.delta").snapshot.max < 0
        metricRegistry.histogram(getClass().name + ".dropEven.delta").snapshot.max == expectedOutBatchSize - batchSize
        metricRegistry.histogram(getClass().name + ".translator.delta").snapshot.max == 0
        metricRegistry.timer(getClass().name + ".dropEven.batch").count == batchCount
        metricRegistry.timer(getClass().name + ".translator.batch").count == batchCount
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

    def "erroneous elements are counted"() {
        given:
        int batchSize = 3
        instance.processingStages = [
                even: { RawEvent event, Object[] params ->
                    if (event.num % 2 == 0) {
                        throw new NullPointerException("test exception that should be ignored")
                    }
                    [event]
                },
                odd : { RawEvent event, Object[] params ->
                    if (event.num % 2 != 0) {
                        throw new NullPointerException("test exception that should be ignored")
                    }
                    [new TranslatedEvent(num: event.num)]
                },
        ]
        when:
        List<TranslatedEvent> translated = instance.translateBatch(
                (1..batchSize).collect { new RawEvent(num: it) },
                new Context(param: "ems"))*.asType(TranslatedEvent)
        then:
        assert translated.size() == 0
        metricRegistry.meter(getClass().name + ".odd.error").count ==
                (batchSize / 2).setScale(0, RoundingMode.UP).longValueExact()
        metricRegistry.meter(getClass().name + ".even.error").count ==
                (batchSize / 2).setScale(0, RoundingMode.DOWN).longValueExact()
    }

}
