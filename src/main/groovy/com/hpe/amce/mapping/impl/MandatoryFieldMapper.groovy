package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.*

import javax.annotation.Nonnull

/**
 * State machine based mapper for mandatory fields.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
class MandatoryFieldMapper<OO, RO, P> extends AbstractStateMachineFieldMapper<OO, RO, P> {

    @Nonnull
    private final State stateMachine

    /**
     * Creates new instance.
     * @param messageFormatterFactory Factory for formatter of error messages or null
     * if {@link SingleLineMesasgeFormatter} should be used.
     */
    // This is configuration of state machine.
    @SuppressWarnings('MethodSize')
    MandatoryFieldMapper(Closure<MessageFormatter> messageFormatterFactory = null) {
        MessageFormatter messageFormatter = messageFormatterFactory ?
                messageFormatterFactory() :
                new SingleLineMesasgeFormatter(mandatory: true)
        Getter getter = new Getter()
        Validator validator = new Validator()
        Translator translator = new Translator()
        Defaulter defaulter = new Defaulter()
        Setter setter = new Setter()
        End end = new End()
        getter.configure(
                validator,
                new WarnIfDefinedOrDataError(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext ctx, MachineContext machine) {
                        messageFormatter.formatMessage(ctx, field, 'Input value absent')
                    }
                },
                new WarnIfDefinedOrDataError(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext ctx, MachineContext machine) {
                        messageFormatter.formatMessage(ctx, field, 'Cannot obtain input value')
                    }
                },
                defaulter)
        validator.configure(
                translator,
                new WarnIfDefinedOrDataError(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext ctx, MachineContext machine) {
                        messageFormatter.formatMessage(ctx, field,
                                "Input value is invalid: '${machine.resultValue}'")
                    }
                },
                new WarnIfDefinedOrDataError(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext ctx, MachineContext machine) {
                        messageFormatter.formatMessage(ctx, field,
                                "Input value cannot be validated: '${machine.resultValue}'")
                    }
                },
                translator)
        translator.configure(
                setter,
                new CodeError('Translator returns null', messageFormatter),
                new WarnIfDefinedOrDataError(defaulter, messageFormatter) {
                    @Override
                    protected String createMessage(Field field, MappingContext ctx, MachineContext machine) {
                        messageFormatter.formatMessage(ctx, field,
                                "Input value cannot be mapped: '${machine.resultValue}'")
                    }
                },
                setter)
        defaulter.configure(
                setter,
                new CodeError('Defaulter returns null, this makes no sense', messageFormatter),
                new CodeError('Defaulter throws exception', messageFormatter),
                new CodeError('Should never see this as should get data error instead' +
                        " by the means of ${WarnIfDefinedOrDataError.simpleName}", messageFormatter))
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
