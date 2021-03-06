package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import groovy.transform.CompileStatic

/**
 * Obtains default value.
 */
@CompileStatic
class Defaulter extends AbstractState {

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        safely(field, mappingContext, machineContext, true) {
            callWithDelegate(field.defaulter, mappingContext)
        }
    }

    @Override
    boolean isDefined(Field field) {
        field.defaulter != null
    }

}
