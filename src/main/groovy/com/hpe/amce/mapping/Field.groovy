package com.hpe.amce.mapping

import groovy.transform.Canonical
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString

import javax.annotation.Nullable

/**
 * Descriptor of a field mapping.
 *
 * Intended to be constructed using fluid API and method chaining.
 * Example:
 * <code><pre>
 *     new Field&lt;Map&lt;String,?&gt;,Event,String,Long&gt;().
 *         withId('notificationIdentifier').
 *         withGetter {it.identifier}.
 *         withTranslator {Long.parseLong(it)}.
 *         withSetter { resultObject.notificationIdentifier=it }* </pre></code>
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * OF - Original field type.
 * RF - Resulting field type.
 * P - Type of parameters object.
 */
@Canonical
// We're referencing type parameters in annotations. Can't make constants out of these
@SuppressWarnings('DuplicateStringLiteral')
class Field<OO, RO, OF, RF, P> {

    /**
     * Identifier of field.
     *
     * Used for debugging and testing purposes.
     */
    String id

    /**
     * Gets field value from original object.
     */
    @Nullable
    Closure<OF> getter

    /**
     * Verifies field value from original object.
     */
    @Nullable
    Closure<Boolean> validator

    /**
     * Translates field value from original object to format of result object.
     */
    @Nullable
    Closure<RF> translator

    /**
     * Injects translated field value into result object.
     */
    @Nullable
    Closure<?> setter

    /**
     * Gets field value in case it was not set in original object.
     */
    @Nullable
    Closure<RF> defaulter

    /**
     * Sets identifier of field.
     * @param id Identifier for debugging and testing purposes.
     * @return this for chaining
     * @see #id
     */
    Field<OO, RO, OF, RF, P> withId(String id) {
        this.id = id
        this
    }

    /**
     * Sets getter of value from original object.
     *
     * @param c Closure that takes one parameter - original object.
     * Should return original field value.
     * @return this for chaining
     */
    Field<OO, RO, OF, RF, P> withGetter(
            @DelegatesTo(type = 'com.hpe.amce.mapping.MappingContext<OO,RO,P>')
            @ClosureParams(value = FromString, options = ['OO'])
                    Closure<OF> c) {
        getter = c
        this
    }

    /**
     * Sets validator for original value.
     *
     * @param c Closure that takes one parameter - original field value.
     * Should return true if original value has acceptable value.
     * @return this for chaining
     */
    Field<OO, RO, OF, RF, P> withValidator(
            @DelegatesTo(type = 'com.hpe.amce.mapping.MappingContext<OO,RO,P>')
            @ClosureParams(value = FromString, options = ['OF'])
                    Closure<Boolean> c) {
        validator = c
        this
    }

    /**
     * Sets mapper from original field value to resulting field value.
     *
     * @param c Closure that takes one parameter - original field value.
     * Additional translator parameters are available via closure delegate.
     * Should return resulting mapped value.
     * @return this for chaining
     */
    Field<OO, RO, OF, RF, P> withTranslator(
            @DelegatesTo(type = 'com.hpe.amce.mapping.MappingContext<OO,RO,P>')
            @ClosureParams(value = FromString, options = ['OF'])
                    Closure<RF> c) {
        translator = c
        this
    }

    /**
     * Sets setter for resulting field.
     *
     * @param c Closure that takes one parameter - resulting field value.
     * Resulting object can be taken from delegate as {@link MappingContext#resultObject}.
     * Doesn't have to return anything.
     * @return this for chaining
     */
    Field<OO, RO, OF, RF, P> withSetter(
            @DelegatesTo(type = 'com.hpe.amce.mapping.MappingContext<OO,RO,P>')
            @ClosureParams(value = FromString, options = ['RF'])
                    Closure<?> c) {
        setter = c
        this
    }

    /**
     * Sets calculator of default value.
     * @param c Closure that takes no parameters and returns resulting value for the field.
     * @return this for chaining.
     */
    Field<OO, RO, OF, RF, P> withDefaulter(
            @DelegatesTo(type = 'com.hpe.amce.mapping.MappingContext<OO,RO,P>')
                    Closure<RF> c) {
        defaulter = c
        this
    }

    @Override
    String toString() {
        id
    }

}
