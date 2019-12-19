package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.MessageFormatter
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * Represents code error.
 * Does not need to be configured.
 */
@CompileStatic
class CodeError extends AbstractState {

    @Nonnull
    private final String errorDescription

    @Nonnull
    private final MessageFormatter messageFormatter

    /**
     * Creates new instance.
     * @param errorDescription Explanation of error.
     * @param messageFormatter Formatter of error message.
     */
    CodeError(@Nonnull String errorDescription, @Nonnull MessageFormatter messageFormatter) {
        assert errorDescription
        assert messageFormatter
        this.errorDescription = errorDescription
        this.messageFormatter = messageFormatter
    }

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        throw new IllegalStateException(
                messageFormatter.formatMessage(mappingContext, field, errorDescription),
                machineContext.error)
    }

    @Override
    boolean isDefined(Field field) {
        true
    }
}

