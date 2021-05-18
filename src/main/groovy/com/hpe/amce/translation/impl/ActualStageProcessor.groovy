package com.hpe.amce.translation.impl

import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Iterates over elements of batch and delegates to {@link AroundElement#translateElement} to actually
 * process each element.
 *
 * C - type of translation context.
 *
 * @see ActualStageProcessor#aroundElement
 */
@CompileStatic
class ActualStageProcessor<C> implements AroundStage<C> {

    /**
     * Applies translation to each element.
     */
    @Nonnull
    final AroundElement<C> aroundElement

    /**
     * Creates an instance.
     * @param aroundElement Processing to apply to each element of a batch.
     */
    ActualStageProcessor(@Nonnull AroundElement<C> aroundElement) {
        this.aroundElement = aroundElement
    }

    @Override
    @Nonnull
    List<?> applyStage(@Nonnull String stageName,
                       @Nonnull Closure<List<?>> stageCode, @Nonnull List<?> elements, @Nullable C context) {
        elements.collectMany { element ->
            aroundElement.translateElement(stageName, stageCode, element, context) ?: Collections.emptyList()
        }
    }
}
