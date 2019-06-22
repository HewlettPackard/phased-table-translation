package com.hpe.amce.mapping

import spock.lang.Specification

class FieldTest extends Specification {

    private static class Original {
        String identifier
    }

    private static class Result {
        Long notificationIdentifier
    }

    private static class Params {
        String parameter
    }

    def "construct via fluent API with closure parameters and delegate help from IDE"() {
        when:
        Field<Original, Result, String, Long, Params> field = new Field<Original, Result, String, Long, Params>().
                withId('notificationIdentifier').
                withGetter { it.identifier }.
                withValidator { it != parameters.parameter }.
                withDefaulter { 777L }.
                withTranslator { Long.parseLong(it) }.
                withSetter { resultObject.notificationIdentifier = it }
        then:
        field.id == "notificationIdentifier"
        field.getter
        field.validator
        field.defaulter
        field.translator
        field.setter
    }

}
