package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

/**
 * Ends processing.
 * Does not need to be configured.
 */
class End extends AbstractState {

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        machineContext.resultValue
    }

    @Override
    boolean isDefined(Field field) {
        true
    }

}
