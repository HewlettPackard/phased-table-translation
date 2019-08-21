package com.hpe.amce.translation.impl

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Calls actual translator to translate element.
 */
class StageCaller<C> implements AroundElement<C> {
    @Override
    List<?> translateElement(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                             @Nullable Object element, @Nullable C context) {
        stageCode(element, context)
    }
}
