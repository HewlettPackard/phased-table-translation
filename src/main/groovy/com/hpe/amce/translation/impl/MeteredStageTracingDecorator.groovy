package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * {@link StageTracingDecorator} that reports how long it took to do the tracing.
 *
 * Both in and out tracing for all stages is tracked using a single {@link com.codahale.metrics.Timer}.
 *
 * C - type of translation context.
 */
class MeteredStageTracingDecorator<C> extends StageTracingDecorator<C> {

    @Nonnull
    private final Timer timer

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param dumper Stage dumper.
     * @param metricRegistry Registry where to report metric.
     * @param metricName Name of metric that will hold tracing time.
     */
    MeteredStageTracingDecorator(AroundStage<C> next, StageDumper<C> dumper, MetricRegistry metricRegistry,
                                 String metricName) {
        super(next, dumper)
        timer = metricRegistry.timer(metricName)
    }

    @Override
    protected void traceIn(@Nonnull String stageName, @Nonnull List<?> elements, @Nullable C context) {
        timer.time { super.traceIn(stageName, elements, context) }
    }

    @Override
    protected void traceOut(@Nonnull String stageName, @Nonnull List<?> elements, @Nullable C context) {
        timer.time { super.traceOut(stageName, elements, context) }
    }
}
