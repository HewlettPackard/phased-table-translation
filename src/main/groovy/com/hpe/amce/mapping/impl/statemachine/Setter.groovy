package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

/**
 * Pushes result value to setter.
 */
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
