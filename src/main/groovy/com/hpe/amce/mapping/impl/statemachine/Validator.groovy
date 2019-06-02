package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

/**
 * Validates result value.
 */
class Validator extends AbstractState {

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        safely(field, mappingContext, machineContext, false) {
            callWithDelegate(field.validator, mappingContext, machineContext.resultValue)
        }
    }

    @Override
    boolean isDefined(Field field) {
        field.validator != null
    }

    @Override
    protected boolean isNull(Object value) {
        !value
    }

}
