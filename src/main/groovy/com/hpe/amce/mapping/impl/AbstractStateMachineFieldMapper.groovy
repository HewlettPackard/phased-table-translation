package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.impl.statemachine.MachineContext
import com.hpe.amce.mapping.impl.statemachine.State
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j2

import javax.annotation.Nonnull

/**
 * Field mapper based on state machine.
 *
 * Child classes should define their state machine using {@link AbstractStateMachineFieldMapper#getStateMachine()}.
 *
 * This base class will check for incorrect configuration combinations:
 * <ul>
 * <li>If there is no setter then defaulter and translation are useless and should not be set.</li>
 * <li>If there is no getter then it's pointless to set
 *      validator or translator because they won't be called anyway.</li>
 * </ul>
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
@Log4j2
@CompileStatic
abstract class AbstractStateMachineFieldMapper<OO, RO, P> implements FieldMapper<OO, RO, P> {

    @Override
    void mapField(
            @Nonnull Field<OO, RO, ?, ?, P> field,
            @Nonnull MappingContext<OO, RO, P> mappingContext) {
        if (!field.setter && (field.defaulter || field.translator)) {
            throw new IllegalStateException('Since there is no setter, defaulter and translator make no sense' +
                    ' as their result will be ignored.')
        }
        if (!field.getter && (field.validator || field.translator)) {
            throw new IllegalStateException("If there is no getter then it's pointless to set " +
                    "validator or translator because they won't be called anyway.")
        }
        if (mappingContext == null) {
            throw new IllegalStateException('Translation context must be set')
        }
        if (field == null) {
            throw new IllegalStateException('Field descriptor must be set')
        }
        getStateMachine(field).process(field, mappingContext, new MachineContext())
    }

    /**
     * Gets state machine for this field mapper.
     * @param field Field to be translated.
     * @return Starting state of state machine.
     */
    protected abstract State getStateMachine(@Nonnull Field<OO, RO, ?, ?, P> field)

}
