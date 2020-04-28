package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import spock.lang.Specification

class PrefixedFieldIdMetricNameCalculatorTest extends Specification {

    def "metric name is a prefixed field id"() {
        given:
        PrefixedFieldIdMetricNameCalculator instance = new PrefixedFieldIdMetricNameCalculator("foo")
        MappingContext mappingContext = Mock()
        Field field = new Field("moo")
        when:
        String result = instance.calculateMetricName(field, mappingContext)
        then:
        result == "foomoo"
    }

    def "survives not set field id"() {
        given:
        PrefixedFieldIdMetricNameCalculator instance = new PrefixedFieldIdMetricNameCalculator("foo")
        MappingContext mappingContext = Mock()
        String name = null
        Field field = new Field(name)
        when:
        String result = instance.calculateMetricName(field, mappingContext)
        then:
        result.contains("foo")
        result != "foo"
    }

}
