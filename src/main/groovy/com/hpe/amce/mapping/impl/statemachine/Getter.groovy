package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

/**
 * Extracts input value.
 */
class Getter extends AbstractState {

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        safely(field, mappingContext, machineContext, true) {
            callWithDelegate(field.getter, mappingContext, mappingContext.originalObject)
        }
    }

    @Override
    boolean isDefined(Field field) {
        field.getter != null
    }

}
