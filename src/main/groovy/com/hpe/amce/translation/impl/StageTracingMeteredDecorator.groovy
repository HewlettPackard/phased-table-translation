package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.MetricSupplier
import com.codahale.metrics.Timer
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * {@link StageTracingDecorator} that reports how long it took to do the tracing.
 *
 * Both in and out tracing for all stages is tracked using a single {@link com.codahale.metrics.Timer}.
 *
 * C - type of translation context.
 */
@CompileStatic
class StageTracingMeteredDecorator<C> extends StageTracingDecorator<C> {

    @Nonnull
    private final Timer timer

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param dumper Stage dumper.
     * @param metricRegistry Registry where to report metric.
     * @param metricName Name of metric that will hold tracing time.
     */
    StageTracingMeteredDecorator(AroundStage<C> next, StageDumper<C> dumper, MetricRegistry metricRegistry,
                                 String metricName) {
        this(next, dumper, metricRegistry, metricName, new MetricSupplier<Timer>() {
            @Override
            Timer newMetric() {
                new Timer()
            }
        })
    }

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param dumper Stage dumper.
     * @param metricRegistry Registry where to report metric.
     * @param metricName Name of metric that will hold tracing time.
     * @param timerFactory Factory to be used to create timer metric.
     */
    StageTracingMeteredDecorator(AroundStage<C> next, StageDumper<C> dumper, MetricRegistry metricRegistry,
                                 String metricName, MetricSupplier<Timer> timerFactory) {
        super(next, dumper)
        timer = metricRegistry.timer(metricName, timerFactory)
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
