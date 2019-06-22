package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.State
import spock.lang.Specification

class AbstractStateMachineFieldMapperTest extends Specification {

    AbstractStateMachineFieldMapper mapper = Spy()

    def "error when Neither getter not defaulter are set - no way to obtain value to validate/translate/set"() {
        when:
        mapper.mapField(new Field().withValidator { true }.withSetter {}, null)
        then:
        thrown(Exception)
    }

    def """error If setter is not set this means we are in pure checking mode.
        This is pointless if validator is not set.
        Also defaulter and translation are useless in this case and should not be set."""() {
        when:
        mapper.mapField(new Field().withGetter {}, null)
        then:
        thrown(Exception)
        when:
        mapper.mapField(new Field().withGetter {}.withValidator { true }.withDefaulter {}, null)
        then:
        thrown(Exception)
        when:
        mapper.mapField(new Field().withGetter {}.withValidator { true }.withTranslator {}, null)
        then:
        thrown(Exception)
    }

    def """If there is no getter then it's pointless to set
        validator or translator because they won't be called anyway."""() {
        when:
        mapper.mapField(new Field().withValidator { true }.withDefaulter {}.withSetter {}, null)
        then:
        thrown(Exception)
        when:
        mapper.mapField(new Field().withTranslator {}.withDefaulter {}.withSetter {}, null)
        then:
        thrown(Exception)
    }

    def "as is from source is ok and delegates to child machine"() {
        given:
        State stateMachine = Mock()
        Field field = new Field().withGetter {}.withSetter {}
        MappingContext mappingContext = Mock()
        when:
        mapper.mapField(field, mappingContext)
        then:
        1 * mapper.stateMachine >> stateMachine
        1 * stateMachine.process(field, mappingContext, !null)
    }

    def "fixed value is ok and delegates to child machine"() {
        given:
        State stateMachine = Mock()
        Field field = new Field().withDefaulter {}.withSetter {}
        MappingContext mappingContext = Mock()
        when:
        mapper.mapField(field, mappingContext)
        then:
        1 * mapper.stateMachine >> stateMachine
        1 * stateMachine.process(field, mappingContext, !null)
    }

    def "pure checking is ok and delegates to child machine"() {
        given:
        State stateMachine = Mock()
        Field field = new Field().withGetter {}.withValidator { true }
        MappingContext mappingContext = Mock()
        when:
        mapper.mapField(field, mappingContext)
        then:
        1 * mapper.stateMachine >> stateMachine
        1 * stateMachine.process(field, mappingContext, !null)
    }
}
