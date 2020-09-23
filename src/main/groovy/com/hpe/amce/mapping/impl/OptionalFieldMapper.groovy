package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.*
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * State machine based mapper for optional fields.
 *
 * Optional means that corresponding field in the resulting object is optional and is not required to be set.
 * Optional field could be used in pure validation mode when there is no field to set in
 * resulting object to generate a warning if check on input value fails.
 *
 * A warning will be generated if optional field
 * has an invalid value (as per validator) or can't be translated.
 * No exception will be raised and nothing will be injected into resulting object
 * unless default value is specified.
 *
 * If there is default value then it will be used if input field is missing,
 * invalid or causes translation error.
 *
 * Absence of optional field in input is not considered as an error and does not generate a warning.
 *
 * Absence means null.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
@CompileStatic
class OptionalFieldMapper<OO, RO, P> extends AbstractStateMachineFieldMapper<OO, RO, P> {

    @Nonnull
    private final State normalStateMachine

    @Nonnull
    private final State ignoreInputFieldStateMachine

    @Nonnull
    private final State ignoreOutputFieldStateMachine

    /**
     * Create state machine for the case when we want to propagate value from input field to output.
     * @param messageFormatter Message formatter to be used.
     * @return State machine to execute.
     */
    private static State createNormalStateMachine(MessageFormatter messageFormatter) {
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
        return getter
    }

    /**
     * Create state machine for the case when we just want to highlight that we ignore certain input field.
     *
     * We're supposed to have a getter to show that we are aware of the field but nothing else
     * to show we are ignoring it.
     *
     * @return State machine to be executed.
     */
    private static State createIgnoreInputFieldStateMachine() {
        new End()
    }

    /**
     * Create state machine for the case when we just want to highlight that we ignore certain output field.
     *
     * We're supposed to have a setter to show that we are aware of the field but nothing else
     * to show we are ignoring it.
     *
     * @return State machine to be executed.
     */
    private static State createIgnoreOutputFieldStateMachine() {
        new End()
    }

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
        normalStateMachine = createNormalStateMachine(messageFormatter)
        ignoreInputFieldStateMachine = createIgnoreInputFieldStateMachine()
        ignoreOutputFieldStateMachine = createIgnoreOutputFieldStateMachine()
    }

    @Override
    protected State getStateMachine(@Nonnull Field<OO, RO, ?, ?, P> field) {
        if (!field.getter && !field.setter) {
            throw new IllegalStateException('There are neither getter, nor setter. Can`t do anything with such field.')
        }
        if (field.getter && !field.validator && !field.setter) {
            if (field.defaulter || field.translator) {
                throw new IllegalStateException('There is getter but no validator or setter' +
                        ' this means we want to show we ignore certain input field.' +
                        ' This means defaulter or translator make no sense because their result will be ignored')
            }
            return ignoreInputFieldStateMachine
        }
        if (!field.getter && !field.defaulter && field.setter) {
            return ignoreOutputFieldStateMachine
        }
        return normalStateMachine
    }
}
