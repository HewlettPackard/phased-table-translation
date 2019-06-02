package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.*

import javax.annotation.Nonnull

/**
 * State machine based mapper for optional fields.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
class OptionalFieldMapper<OO, RO, P> extends AbstractStateMachineFieldMapper<OO, RO, P> {

    @Nonnull
    private final State stateMachine

    /**
     * Creates new instance.
     * @param messageFormatterFactory Factory for formatter of error messages or null
     * if {@link SingleLineMesasgeFormatter} should be used.
     */
    // This is configuration of state machine.
    @SuppressWarnings('MethodSize')
    OptionalFieldMapper(Closure<MessageFormatter> messageFormatterFactory = null) {
        MessageFormatter messageFormatter = messageFormatterFactory ?
                messageFormatterFactory() :
                new SingleLineMesasgeFormatter(mandatory: false)
        Getter getter = new Getter()
        Validator validator = new Validator()
        Translator translator = new Translator()
        Defaulter defaulter = new Defaulter()
        Setter setter = new Setter()
        End end = new End()
        getter.configure(
                validator,
                defaulter,
                new Warn(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext mappingContext,
                                                   MachineContext machineContext) {
                        messageFormatter.formatMessage(mappingContext, field, 'Cannot obtain input value')
                    }
                },
                defaulter)
        validator.configure(
                translator,
                new Warn(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext mappingContext,
                                                   MachineContext machineContext) {
                        messageFormatter.formatMessage(mappingContext, field,
                                "Input value is invalid: '${machineContext.resultValue}'")
                    }
                },
                new Warn(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext mappingContext,
                                                   MachineContext machineContext) {
                        messageFormatter.formatMessage(mappingContext, field,
                                "Input value cannot be validated: '${machineContext.resultValue}'")
                    }
                },
                translator)
        translator.configure(
                setter,
                setter,
                new Warn(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext mappingContext,
                                                   MachineContext machineContext) {
                        messageFormatter.formatMessage(mappingContext, field,
                                "Input value cannot be mapped: '${machineContext.resultValue}'")
                    }
                },
                setter)
        defaulter.configure(
                setter,
                new CodeError('Defaulter returns null, this makes no sense', messageFormatter),
                new CodeError('Defaulter throws exception', messageFormatter),
                end)
        setter.configure(
                end,
                end,
                new CodeError('Setter throws exception', messageFormatter),
                end)
        stateMachine = getter
    }

    @Override
    protected State getStateMachine() {
        stateMachine
    }
}
