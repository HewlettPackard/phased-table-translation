package com.hpe.amce.translation.impl

import com.hpe.amce.translation.BatchTranslator
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Iterates over list of processing stages and delegates to {@link AroundStage#applyStage} for each of them.
 *
 * Each next stage gets input produced by previous stage.
 *
 * O - type of source elements.
 * R - type of result elements.
 * C - type of translation context.
 *
 * @see StagesCaller#processingStages
 * @see StagesCaller#aroundStage
 */
@CompileStatic
class StagesCaller<O, R, C> implements BatchTranslator<O, R, C> {

    /**
     * Processing stages.
     * <br/>
     * Keys are stage names and entries are closures that do the processing.
     * <br/>
     * The closures should take two parameters.
     * Where first parameter is a single event being processed.
     * Its type should be a type of raw event for the first stage.
     * For the second and further stages, its type should be the same
     * as output of previous stage. For example, if previous stage
     * is just a filter on raw events then for the current stage,
     * the first parameter should also be of raw event type.
     * Second parameter has type C and represents context (extra parameters) passed to
     * {@link #translateBatch}.
     * <br/>
     * The closure should return list of processed events (zero if filtered out,
     * exactly one for one-to-one translation, more than one if any extra
     * events are to be injected).
     * <br/>
     * Stages are called in whatever order map iterates them so use ordered maps.
     */
    @Nonnull
    final Map<String, Closure<List<?>>> processingStages

    /**
     * Applies translation stage to a batch of elements.
     */
    @Nonnull
    final AroundStage<C> aroundStage

    /**
     * Creates an instance.
     * @param processingStages Definition of processing stages.
     * @param aroundStage What to do for each stage.
     * @see StagesCaller#processingStages
     * @see StagesCaller#aroundStage
     */
    StagesCaller(Map<String, Closure<List<?>>> processingStages, AroundStage<C> aroundStage) {
        this.processingStages = processingStages
        this.aroundStage = aroundStage
    }

    @Override
    List<R> translateBatch(@Nullable List<O> elements, @Nullable C context) {
        List<?> result = elements
        processingStages.each { stageName, stageCode ->
            result = result != null ? aroundStage.applyStage(stageName, stageCode, result, context) : []
        }
        result
    }
}
