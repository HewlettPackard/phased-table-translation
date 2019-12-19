package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import groovy.transform.CompileStatic

/**
 * Translates result value.
 */
@CompileStatic
class Translator extends AbstractState {

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        safely(field, mappingContext, machineContext, true) {
            callWithDelegate(field.translator, mappingContext, machineContext.resultValue)
        }
    }

    @Override
    boolean isDefined(Field field) {
        field.translator != null
    }

}
