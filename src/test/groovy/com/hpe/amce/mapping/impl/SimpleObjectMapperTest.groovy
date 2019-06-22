package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import spock.lang.Specification

class SimpleObjectMapperTest extends Specification {

    FieldMapper mandatoryFieldMapper = Mock()
    FieldMapper optionalFieldMapper = Mock()
    SimpleObjectMapper objectMapper = new SimpleObjectMapper(
            mandatoryFieldMapper: mandatoryFieldMapper,
            optionalFieldMapper: optionalFieldMapper)

    def "delegates to optional mapper for optional field"() {
        given:
        Field field = new Field()
        Map<Field, Boolean> fields = [(field): false]
        Object input = Mock()
        Object result = Mock()
        Object parameters = Mock()
        when:
        objectMapper.mapAllFields(input, result, fields, parameters)
        then:
        1 * optionalFieldMapper.mapField(field, { MappingContext context ->
            context != null &&
                    context.parameters == parameters &&
                    context.originalObject == input &&
                    context.resultObject == result
        })
        then:
        0 * _
    }

    def "delegates to mandatory mapper for mandatory field"() {
        given:
        Field field = new Field()
        Map<Field, Boolean> fields = [(field): true]
        Object input = Mock()
        Object result = Mock()
        Object parameters = Mock()
        when:
        objectMapper.mapAllFields(input, result, fields, parameters)
        then:
        1 * mandatoryFieldMapper.mapField(field, { MappingContext context ->
            context != null &&
                    context.parameters == parameters &&
                    context.originalObject == input &&
                    context.resultObject == result
        })
        then:
        0 * _
    }

    def "processes all fields in specified order"() {
        given:
        Field field1 = new Field().withId("b")
        Field field2 = new Field().withId("c")
        Field field3 = new Field().withId("a")
        Map<Field, Boolean> fields = [
                (field1): true,
                (field2): false,
                (field3): true,
        ]
        Object input = Mock()
        Object result = Mock()
        Object parameters = Mock()
        when:
        objectMapper.mapAllFields(input, result, fields, parameters)
        then:
        1 * mandatoryFieldMapper.mapField(field1, { MappingContext context ->
            context != null &&
                    context.parameters == parameters &&
                    context.originalObject == input &&
                    context.resultObject == result
        })
        then:
        1 * optionalFieldMapper.mapField(field2, { MappingContext context ->
            context != null &&
                    context.parameters == parameters &&
                    context.originalObject == input &&
                    context.resultObject == result
        })
        then:
        1 * mandatoryFieldMapper.mapField(field3, { MappingContext context ->
            context != null &&
                    context.parameters == parameters &&
                    context.originalObject == input &&
                    context.resultObject == result
        })
        then:
        0 * _
    }

    def "uses default field mappers by default"() {
        when:
        objectMapper = new SimpleObjectMapper()
        then:
        objectMapper.mandatoryFieldMapper instanceof MandatoryFieldMapper
        objectMapper.optionalFieldMapper instanceof OptionalFieldMapper
    }
}
