package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.MessageFormatter
import groovy.transform.CompileStatic

/**
 * Warn if next step is defined, throw otherwise.
 *
 * If next (delegate) step is configured then works like {@link Warn}.
 * If next step is not defined then instead it throws data error with the same message
 * as would be logged.
 */
@CompileStatic
// There is abstract method: createMessage
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class WarnIfDefinedOrDataError extends Warn {

    /**
     * Creates instance.
     * @param delegate Step to delegate to after logging.
     * @param mapper Mapper used to log message.
     */
    protected WarnIfDefinedOrDataError(State delegate, MessageFormatter messageFactory) {
        super(delegate, messageFactory)
    }

    @Override
    Object process(Field field, MappingContext mappingContext, MachineContext machineContext) {
        if (!isDefined(field)) {
            throw new IllegalArgumentException(
                    createMessage(field, mappingContext, machineContext),
                    machineContext.error)
        }
        super.process(field, mappingContext, machineContext)
    }

}
