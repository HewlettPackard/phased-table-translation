package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j2

import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.util.concurrent.Callable

/**
 * Decorates another {@link AroundStage}
 * to report metrics for each stage.
 *
 * The following metrics will be published:
 * <ul>
 *     <li>Size difference for a batch before and after a stage (how much was filtered out or added)</li>
 *     <li>Time it takes to process a batch on each stage</li>
 * </ul>
 *
 * Metrics will be published in {@link StageMeteringDecorator#metricRegistry} with
 * {@link StageMeteringDecorator#metricsBaseName} being the prefix for the name of
 * all metrics.
 *
 * It is possible to override default metric names via
 * {@link StageMeteringDecorator#deltaBatchSizeMetricName},
 * {@link StageMeteringDecorator#stageTimerMetricName}.
 *
 * C - type of translation context.
 */
@Log4j2
@CompileStatic
class StageMeteringDecorator<C> implements AroundStage<C> {

    /**
     * Translator being decorated.
     */
    @Nonnull
    AroundStage<C> next

    /**
     * Metric registry to which metrics should be reported.
     */
    @Nonnull
    MetricRegistry metricRegistry

    /**
     * Base name for reported metrics.
     */
    @Nonnull
    String metricsBaseName

    /**
     * Name of the metric that will report batch size delta for a stage.
     *
     * Zero means that stage did not change number of elements. Positive value means that
     * stage has added elements. Negative value means that stage has removed elements.
     *
     * The closure should take stage name as parameter and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}STAGENAME.delta.
     */
    @Nonnull
    Closure<String> deltaBatchSizeMetricName = { String stageName -> "$metricsBaseName${stageName}.delta".toString() }

    /**
     * Name of the metric that will report time it took to process a batch on a stage.
     *
     * The closure should take stage name as parameter and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}STAGENAME.batch.
     */
    @Nonnull
    Closure<String> stageTimerMetricName =
            { String stageName -> "$metricsBaseName${stageName}.batch".toString().toString() }

    /**
     * Creates new instance.
     * @param next Translator to decorate.
     * @param metricRegistry Registry where to report metrics.
     * @param metricsBaseName Prefix for metric names.
     */
    StageMeteringDecorator(@Nonnull AroundStage<C> next, @Nonnull MetricRegistry metricRegistry,
                           @Nonnull String metricsBaseName) {
        this.next = next
        this.metricRegistry = metricRegistry
        this.metricsBaseName = metricsBaseName
    }

    @Override
    List<?> applyStage(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                       @Nonnull List<?> elements, @Nullable C context) {
        List<?> result = metricRegistry.timer(stageTimerMetricName(stageName)).time({
            next.applyStage(stageName, stageCode, elements, context)
        } as Callable<List<?>>)
        metricRegistry.histogram(deltaBatchSizeMetricName(stageName)).update(
                (result?.size() ?: 0) - (elements?.size() ?: 0))
        result
    }
}
