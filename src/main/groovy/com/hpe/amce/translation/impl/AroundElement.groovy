package com.hpe.amce.translation.impl

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Defines what to do for each element of a batch.
 *
 * C - type of translation context.
 */
interface AroundElement<C> {
    /**
     * Applies specified translation stage to a batch of elements.
     * @param stageName Name of translation stage to be applied.
     * @param stageCode Code of translation stage to be applied.
     * @param element Element to be translated. Can be null if null was passed.
     * @param context Translation context or null if not specified.
     * @return Result of translating specified element using specified stage.
     * See {@link StagesCaller#processingStages}.
     */
    @Nonnull
    List<?> translateElement(@Nonnull String stageName,
                             @Nonnull Closure<List<?>> stageCode, @Nullable Object element, @Nullable C context)
}
