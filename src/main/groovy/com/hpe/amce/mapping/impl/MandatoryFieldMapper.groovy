package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.*
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * State machine based mapper for mandatory fields.
 *
 * Mandatory means that corresponding field in the resulting object must be set.
 * However, in pure validation mode when there is no field to set in
 * resulting object, mandatory means that input value must be present and must be valid.
 *
 * It is considered to be data error if mandatory field is absent in input
 * or has an invalid value (as per validator) or can't be translated.
 * Data error means that an exception will be raised.
 *
 * If there is default value then it will be used if input field is missing,
 * invalid or causes translation error. However, warning message will still
 * be generated.
 *
 * If either getter not defaulter are set - no way to obtain value to validate/translate/set.
 * This is treated as code error.
 *
 * If we have a getter then there supposed to be either validator to check input value or
 * setter to propagate it to output. If there are none then the field should not be mandatory.
 *
 * Absence means null.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
@CompileStatic
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
    protected State getStateMachine(@Nonnull Field<OO, RO, ?, ?, P> field) {
        if (!field.getter && !field.defaulter) {
            throw new IllegalStateException('Neither getter not defaulter are set' +
                    ' - no way to obtain value to validate/translate/set.')
        }
        if (field.getter && !field.validator && !field.setter) {
            throw new IllegalStateException('There is a getter but neither validator, nor setter' +
                    ' so we neither validate, nor propagate it - this makes no sense for mandatory field.')
        }
        stateMachine
    }

}
