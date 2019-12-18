package com.hpe.amce.translation

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Translates a batch of elements.
 *
 * O - type of source elements.
 * R - type of result elements.
 * C - type of translation context.
 */
interface BatchTranslator<O, R, C> {
    /**
     * Translates a batch of elements.
     * @param elements Elements to translate.
     * @param context Translation context.
     * @return Translated elements.
     */
    @Nonnull
    List<R> translateBatch(@Nullable List<O> elements, @Nullable C context)
}
