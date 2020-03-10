package com.hpe.amce.translation.impl

import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Calls actual translator to translate element.
 *
 * Actual translator means closure specified for a particular stage.
 */
@CompileStatic
class StageCaller<C> implements AroundElement<C> {
    @Override
    List<?> translateElement(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                             @Nullable Object element, @Nullable C context) {
        stageCode(element, context)
    }
}
