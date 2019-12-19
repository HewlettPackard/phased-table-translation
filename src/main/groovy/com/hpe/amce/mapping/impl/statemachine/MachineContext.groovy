package com.hpe.amce.mapping.impl.statemachine

import groovy.transform.CompileStatic

import javax.annotation.Nullable

/**
 * Intermediate state of a state machine as we transition between states.
 */
@CompileStatic
class MachineContext {

    /**
     * Current result.
     */
    @Nullable
    Object resultValue

    /**
     * An error that has happened.
     */
    @Nullable
    Throwable error

}
