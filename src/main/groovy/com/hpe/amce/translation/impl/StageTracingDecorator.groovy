package com.hpe.amce.translation.impl

import groovy.transform.CompileStatic
import groovy.util.logging.Log4j2
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Logger

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Decorates another {@link AroundStage}
 * to trace a batch before and after each stage.
 *
 * Level of logging before and after a stage can be controlled via
 * {@link StageTracingDecorator#inLevel} and {@link StageTracingDecorator#outLevel}.
 *
 * How a batch is dumped is controlled via {@link StageTracingDecorator#dumper}.
 *
 * Which logger is used to to trace before and after each stage is controlled via
 * {@link StageTracingDecorator#findLoggerForStageAndMode}.
 *
 * C - type of translation context.
 */
@Log4j2
@CompileStatic
class StageTracingDecorator<C> implements AroundStage<C> {

    /**
     * Translator being decorated.
     */
    @Nonnull
    AroundStage<C> next

    /**
     * Level at which batch will be traced before translation stage is applied.
     *
     * This is TRACE by default.
     */
    @Nonnull
    Level inLevel = Level.TRACE

    /**
     * Level at which batch will be traced after translation stage is applied.
     *
     * This is TRACE by default.
     */
    @Nonnull
    Level outLevel = Level.TRACE

    /**
     * Dumper for all stages.
     */
    @Nonnull
    StageDumper<C> dumper

    /**
     * Mapping between stages and loggers.
     *
     * See {@link #findLoggerForStageAndMode} for details.
     */
    @Nullable
    Map<String, Logger> loggers

    /**
     * Controls which logger to be used to trace before and after each stage.
     *
     * The closure should take two parameters:
     * <ol>
     *     <li>String stage - name of stage being traced.</li>
     *     <li>boolean isIn - true if tracing before stage, false if tracing after stage.</li>
     * </ol>
     *
     * The closure should return a logger that should be used to trace a batch.
     *
     * By default, it will look for logger in {@link #loggers}. If not found then
     * logger for {@link StageTracingDecorator} will be used. By default, the key should be
     * stageName.in for logger to be used before stage with name stageName and
     * stageName.out for logger to be used after stage with name stageName.
     */
    @Nonnull
    Closure<Logger> findLoggerForStageAndMode = { @Nonnull String stage, boolean isIn ->
        if (!loggers) {
            return log
        }
        loggers[stage + (isIn ? '.in' : '.out')] ?: log
    }

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param dumper Stage dumper.
     */
    StageTracingDecorator(
            @Nonnull AroundStage<C> next,
            @Nonnull StageDumper<C> dumper) {
        this.next = next
        this.dumper = dumper
    }

    @Override
    List<?> applyStage(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                       @Nonnull List<?> elements, @Nullable C context) {
        traceIn(stageName, elements, context)
        List<?> result = next.applyStage(stageName, stageCode, elements, context)
        traceOut(stageName, result, context)
        result
    }

    /**
     * Traces input elements.
     * @param stageName Name of stage being traced.
     * @param elements Stage input elements.
     * @param context Translation context.
     */
    protected void traceIn(@Nonnull String stageName,
                           @Nonnull List<?> elements, @Nullable C context) {
        Logger inLogger = findLoggerForStageAndMode(stageName, true)
        if (inLogger.isEnabled(inLevel)) {
            inLogger.log(inLevel, dumper.dumpBeforeStage(stageName, elements, context))
        }
    }

    /**
     * Traces output elements.
     * @param stageName Name of stage being traced.
     * @param elements Stage output elements.
     * @param context Translation context.
     */
    protected void traceOut(@Nonnull String stageName,
                            @Nonnull List<?> elements, @Nullable C context) {
        Logger outLogger = findLoggerForStageAndMode(stageName, false)
        if (outLogger.isEnabled(outLevel)) {
            outLogger.log(outLevel, dumper.dumpAfterStage(stageName, elements, context))
        }
    }
}
