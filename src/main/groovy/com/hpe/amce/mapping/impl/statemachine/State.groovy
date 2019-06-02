package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * State in field translation state machine.
 */
interface State {

    /**
     * Is current step configured in field configuration?
     * @param field Field configuration.
     * @return true if current step is configured for a field.
     */
    boolean isDefined(@Nonnull Field field)

    /**
     * Do the work related to the step.
     * @param field Definition of the field.
     * @param mappingContext Mapping context.
     * @param machineContext State machine context.
     * @return Result of current step evaluation.
     */
    @Nullable
    Object process(
            @Nonnull Field field,
            @Nonnull MappingContext mappingContext,
            @Nonnull MachineContext machineContext)

}
