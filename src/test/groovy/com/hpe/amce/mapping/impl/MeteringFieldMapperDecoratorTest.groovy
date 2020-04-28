package com.hpe.amce.mapping.impl

import com.codahale.metrics.MetricRegistry
import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import spock.lang.Specification

class MeteringFieldMapperDecoratorTest extends Specification {

    private class Original {}

    private class Result {}

    private class Params {}

    MetricRegistry metricRegistry = new MetricRegistry()

    FieldMapper<Original, Result, Params> delegate = Mock()

    MeteringFieldMapperDecorator.MetricNameCalculator metricNameCalculator = Mock()

    FieldMapper<Original, Result, Params> instance =
            new MeteringFieldMapperDecorator<>(delegate, metricRegistry, metricNameCalculator)

    Field<Original, Result, Object, Object, Params> fieldDescriptor = Mock()

    MappingContext<Original, Result, Params> mappingContext = Mock()

    def "delegates and times"() {
        when:
        instance.mapField(fieldDescriptor, mappingContext)
        then:
        1 * metricNameCalculator.calculateMetricName(fieldDescriptor, mappingContext) >> "foo"
        1 * delegate.mapField(fieldDescriptor, mappingContext)
        metricRegistry.timer("foo").count == 1
    }

    def "metric name is per field"() {
        when:
        instance.mapField(fieldDescriptor, mappingContext)
        instance.mapField(fieldDescriptor, mappingContext)
        then:
        2 * metricNameCalculator.calculateMetricName(fieldDescriptor, mappingContext) >>> ["foo", "moo"]
        2 * delegate.mapField(fieldDescriptor, mappingContext)
        metricRegistry.timer("foo").count == 1
        metricRegistry.timer("moo").count == 1
    }

}
