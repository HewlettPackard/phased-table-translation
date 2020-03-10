package com.hpe.amce.translation.impl

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Dumps to string a batch of elements at specified stage.
 *
 * C - type of translation context.
 *
 * @see StageDumper#dumpBeforeStage
 * @see StageDumper#dumpAfterStage
 */
interface StageDumper<C> {

    /**
     * Dumps batch before specified stage.
     * @param stageName Name of stage.
     * @param batch Batch of elements or null if none.
     * @param context Translation context or null if none.
     * @return String representation of parameters.
     */
    @Nonnull
    String dumpBeforeStage(@Nonnull String stageName, @Nullable List<?> batch, @Nullable C context)

    /**
     * Dumps batch after specified stage.
     * @param stageName Name of stage.
     * @param batch Batch of elements or null if none.
     * @param context Translation context or null if none.
     * @return String representation of parameters.
     */
    @Nonnull
    String dumpAfterStage(@Nonnull String stageName, @Nullable List<?> batch, @Nullable C context)
}
