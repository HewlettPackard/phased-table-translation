package com.hpe.amce.translation.impl

import spock.lang.Specification

class DecorableStagedBatchTranslatorTest extends Specification {

    def "can handle null input batch"() {
        given:
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [test: { input, context -> ['something'] }]
        }
        when:
        List result = instance.translateBatch(null, null)
        then:
        result == []
    }
}
