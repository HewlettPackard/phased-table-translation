package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import groovy.transform.CompileStatic

/**
 * Pushes result value to setter.
 */
@CompileStatic
class Setter extends AbstractState {

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        safely(field, mappingContext, machineContext, false) {
            callWithDelegate(field.setter, mappingContext, machineContext.resultValue)
        }
    }

    @Override
    boolean isDefined(Field field) {
        field.setter != null
    }

}
