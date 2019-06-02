package com.hpe.amce.mapping.impl.statemachine

import javax.annotation.Nullable

/**
 * Intermediate state of a state machine as we transition between states.
 */
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
