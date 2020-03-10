package com.hpe.amce.translation.impl

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Defines what to do for each translation stage.
 *
 * @see AroundStage#applyStage
 */
interface AroundStage<C> {
    /**
     * Applies specified translation stage to a batch of elements.
     * @param stageName Name of translation stage to be applied.
     * @param stageCode Code of translation stage to be applied.
     * @param elements Batch of elements to be translated.
     * @param context Translation context or null if not specified.
     * @return Result of translating elements using specified stage.
     */
    @Nonnull
    List<?> applyStage(@Nonnull String stageName,
                       @Nonnull Closure<List<?>> stageCode, @Nonnull List<?> elements, @Nullable C context)
}
