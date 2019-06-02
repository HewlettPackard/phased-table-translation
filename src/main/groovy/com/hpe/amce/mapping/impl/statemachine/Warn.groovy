package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.MessageFormatter
import groovy.util.logging.Log4j2

import javax.annotation.Nonnull

/**
 * Pushes a warning to log and then delegates to another step.
 */
@Log4j2
abstract class Warn extends AbstractState {

    @Nonnull
    private final State delegate

    @Nonnull
    private final MessageFormatter messageFormatter

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        if (machineContext.error) {
            log.warn(createMessage(field, mappingContext, machineContext), machineContext.error)
        } else {
            log.warn(createMessage(field, mappingContext, machineContext))
        }
        delegate.process(field, mappingContext, machineContext)
    }

    @Override
    boolean isDefined(Field field) {
        delegate.isDefined(field)
    }

    /**
     * Creates instance.
     * @param delegate Step to delegate to after logging.
     * @param mapper Mapper used to log message.
     */
    protected Warn(@Nonnull State delegate, @Nonnull MessageFormatter messageFormatter) {
        assert delegate
        assert messageFormatter
        this.delegate = delegate
        this.messageFormatter = messageFormatter
    }

    /**
     * Create message to be logged.
     * @param field Field configuration.
     * @param mappingContext Translation context.
     * @param machineContext State machine context.
     * @return Message to be logged.
     */
    abstract protected String createMessage(Field field, MappingContext mappingContext, MachineContext machineContext)

}
