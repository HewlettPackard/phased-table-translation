package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import com.hpe.amce.translation.BatchTranslator
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * {@link BatchTracingDecorator} that reports how long it took to do the tracing.
 *
 * Both in and out tracing is tracked using a single {@link com.codahale.metrics.Timer}.
 *
 * O - type of elements in original batch.
 * R - type of elements in resulting batch.
 * C - type of translation context.
 */
@CompileStatic
class BatchTracingMeteredDecorator<O, R, C> extends BatchTracingDecorator<O, R, C> {

    @Nonnull
    private final com.codahale.metrics.Timer timer

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param inDumper Dumper for original batch.
     * @param outDumper Dumper for resulting batch.
     * @param metricRegistry Registry where to report metric.
     * @param metricName Name of metric that will hold tracing time.
     */
    BatchTracingMeteredDecorator(
            BatchTranslator<O, R, C> next,
            BatchDumper<O, C> inDumper,
            BatchDumper<R, C> outDumper,
            MetricRegistry metricRegistry,
            String metricName) {
        super(next, inDumper, outDumper)
        timer = metricRegistry.timer(metricName)
    }

    @Override
    protected void traceIn(@Nullable List<O> elements, @Nullable C context) {
        timer.time { super.traceIn(elements, context) }
    }

    @Override
    protected void traceOut(@Nullable List<R> elements, @Nullable C context) {
        timer.time { super.traceOut(elements, context) }
    }
}
