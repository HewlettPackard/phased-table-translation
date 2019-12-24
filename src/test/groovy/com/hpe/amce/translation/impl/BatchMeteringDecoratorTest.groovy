package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import com.hpe.amce.translation.BatchTranslator
import spock.lang.Specification

class BatchMeteringDecoratorTest extends Specification {

    def "delegates and publishes metrics"() {
        given:
        String baseName = getClass().name
        BatchTranslator<String, Integer, List<Boolean>> decorated = Mock()
        MetricRegistry metricRegistry = new MetricRegistry()
        List<Boolean> context = [true]
        List<List<String>> original = [['1', '2', '3', '4'], ['5', '6', '7'], ['8', '9']]
        when:
        BatchMeteringDecorator<String, Integer, List<Boolean>> instance = new BatchMeteringDecorator<>(
                decorated, metricRegistry, baseName)
        List<List<Integer>> result = original.collect { instance.translateBatch(it, context) }
        then: "returns result from delegate"
        decorated.translateBatch(_, _) >> { List<String> mockInput, List<Boolean> mockContext ->
            assert mockContext.first()
            mockInput*.asType(Integer) << 0
        }
        result == original.collect { it*.asType(Integer) << 0 }
        then: "publishes 'Received number of batches - meter' using base name"
        metricRegistry.meter(baseName + '.batches.count').count == original.size()
        then: "publishes 'Size of incoming batches - histogram' using base name"
        metricRegistry.histogram(baseName + '.in.batch_size').with {
            assert snapshot.max == original*.size().max()
            assert snapshot.min == original*.size().min()
        }
        then: "publishes 'Total number of incoming elements across all batches - meter' using base name"
        metricRegistry.meter(baseName + '.in.events').count == original*.size().sum()
        then: "publishes 'Time it takes to translate a batch - timer' using base name"
        metricRegistry.timer(baseName + '.translate_batch').count == original.size()
        then: "publishes 'Size of resulting batches - histogram' using base name"
        metricRegistry.histogram(baseName + '.out.batch_size').with {
            assert snapshot.max == original*.size().max() + 1
            assert snapshot.min == original*.size().min() + 1
        }
        then: "publishes 'Total number of resulting elements across all batches - meter' using base name"
        metricRegistry.meter(baseName + '.out.events').count == original*.size().sum() + original.size()
    }

    def "is null tolerant"() {
        given:
        String baseName = getClass().name
        BatchTranslator<String, Integer, List<Boolean>> decorated = Mock()
        MetricRegistry metricRegistry = new MetricRegistry()
        when:
        BatchMeteringDecorator<String, Integer, List<Boolean>> instance = new BatchMeteringDecorator<>(
                decorated, metricRegistry, baseName)
        List<Integer> result = instance.translateBatch(null, null)
        then: "returns result from delegate"
        decorated.translateBatch(null, null) >> null
        result == null
        then: "publishes 'Received number of batches - meter' using base name"
        metricRegistry.meter(baseName + '.batches.count').count == 1
        then: "publishes 'Size of incoming batches - histogram' using base name"
        metricRegistry.histogram(baseName + '.in.batch_size').with {
            assert count == 1
            assert snapshot.max == 0
            assert snapshot.min == 0
        }
        then: "publishes 'Total number of incoming elements across all batches - meter' using base name"
        metricRegistry.meter(baseName + '.in.events').count == 0
        then: "publishes 'Time it takes to translate a batch - timer' using base name"
        metricRegistry.timer(baseName + '.translate_batch').count == 1
        then: "publishes 'Size of resulting batches - histogram' using base name"
        metricRegistry.histogram(baseName + '.out.batch_size').with {
            assert count == 1
            assert snapshot.max == 0
            assert snapshot.min == 0
        }
        then: "publishes 'Total number of resulting elements across all batches - meter' using base name"
        metricRegistry.meter(baseName + '.out.events').count == 0
    }
}
