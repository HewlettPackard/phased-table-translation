package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.MachineContext
import com.hpe.amce.mapping.impl.statemachine.State
import groovy.util.logging.Log4j2

import javax.annotation.Nonnull

/**
 * Field mapper based on state machine.
 *
 * Child classes should define their state machine using {@link AbstractStateMachineFieldMapper#getStateMachine()}.
 *
 * This base class will check for incorrect configuration combinations like
 * like both setter and defaulter being absent at the same time.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
@Log4j2
abstract class AbstractStateMachineFieldMapper<OO, RO, P> implements FieldMapper<OO, RO, P> {

    @Override
    void mapField(
            @Nonnull Field<OO, RO, ?, ?, P> field,
            @Nonnull MappingContext<OO, RO, P> mappingContext) {
        if (!field.getter && !field.defaulter) {
            throw new IllegalStateException('Neither getter, nor defaulter are set. What will I translate?')
        }
        if (!field.setter && (!field.validator || field.defaulter || field.translator)) {
            throw new IllegalStateException('If setter is not set then we are in pure checking mode. ' +
                    'This is pointless if validator is not set. ' +
                    'Also defaulter and translation are useless in this case.')
        }
        if (!field.getter && (field.validator || field.translator)) {
            throw new IllegalStateException("If we have no getter then it's pointless to set validator or translator " +
                    "because they won't be called anyway.")
        }
        if (mappingContext == null) {
            throw new IllegalStateException('Translation context must be set')
        }
        if (field == null) {
            throw new IllegalStateException('Field descriptor must be set')
        }
        stateMachine.process(field, mappingContext, new MachineContext())
    }

    /**
     * Gets state machine for this field mapper.
     * @return Starting state of state machine.
     */
    protected abstract State getStateMachine()

}