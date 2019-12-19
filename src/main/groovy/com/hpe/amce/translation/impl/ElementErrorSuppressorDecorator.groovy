package com.hpe.amce.translation.impl

import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Suppresses any error generated by decorated translator.
 *
 * This can be used to ignore element-level errors so the rest of the batch
 * can be processed.
 *
 * When error is caught, an empty list will be returned effectively making
 * element to be ignored.
 *
 * If you plan to log or somehow report exceptions generated by translators then
 * use this translator first in the chain. Otherwise, other translators will never see an error.
 *
 * C - type of translation context.
 */
@CompileStatic
class ElementErrorSuppressorDecorator<C> implements AroundElement<C> {

    @Nonnull
    private final AroundElement<C> next

    /**
     * Creates instance.
     * @param next Translator to be decorated.
     */
    ElementErrorSuppressorDecorator(@Nonnull AroundElement<C> next) {
        this.next = next
    }

    @Override
    // Want to catch Groovy assertion violations
    @SuppressWarnings('CatchThrowable')
    List<?> translateElement(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                             @Nullable Object element, @Nullable C context) {
        try {
            next.translateElement(stageName, stageCode, element, context)
        } catch (Throwable e) {
            []
        }
    }
}
