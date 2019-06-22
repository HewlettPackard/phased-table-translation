package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import spock.lang.Specification

class SingleLineMesasgeFormatterTest extends Specification {

    SingleLineMesasgeFormatter formatter = new SingleLineMesasgeFormatter()

    def "tells if field is mandatory"() {
        given:
        formatter.mandatory = true
        when:
        String result = formatter.formatMessage(Mock(MappingContext), Mock(Field), "message")
        then:
        result =~ "(?i)mandatory"
    }

    def "tells if field is optional"() {
        when:
        String result = formatter.formatMessage(Mock(MappingContext), Mock(Field), "message")
        then:
        result =~ "(?i)optional"
    }

    def "dumps all info"() {
        given:
        MappingContext mappingContext = Mock()
        Field field = Mock()
        when:
        String result = formatter.formatMessage(mappingContext, field, "message")
        then:
        1 * mappingContext.originalObject >> "original"
        1 * mappingContext.parameters >> "parameters"
        1 * field.id >> "fieldId"
        result.contains("original")
        result.contains("parameters")
        result.contains("fieldId")
        result.contains("message")
    }

    def "produces single line"() {
        when:
        String result = formatter.formatMessage(Mock(MappingContext), Mock(Field), "message")
        then:
        result.readLines().size() == 1
    }
}
