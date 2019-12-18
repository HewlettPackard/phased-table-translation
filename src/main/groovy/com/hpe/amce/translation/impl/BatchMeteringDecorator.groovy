package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import com.hpe.amce.translation.BatchTranslator

import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.util.concurrent.Callable

/**
 * Decorates another {@link BatchTranslator} to publish metrics about incoming and resulting batches.
 *
 * The following metrics will be published:
 * <ul>
 *     <li>Received number of batches</li>
 *     <li>Size of incoming batches</li>
 *     <li>Total number of incoming elements across all batches</li>
 *     <li>Time it takes to translate a batch</li>
 *     <li>Size of resulting batches</li>
 *     <li>Total number of resulting elements across all batches</li>
 * </ul>
 *
 * Metrics will be published in {@link BatchMeteringDecorator#metricRegistry} with
 * {@link BatchMeteringDecorator#metricsBaseName} being the prefix for the name of
 * all metrics.
 *
 * It is possible to override default metric names via
 * {@link BatchMeteringDecorator#incomingBatchCountMetricName},
 * {@link BatchMeteringDecorator#incomingBatchSizeMetricName},
 * {@link BatchMeteringDecorator#incomingElementsCountMetricName},
 * {@link BatchMeteringDecorator#translationTimeMetricName},
 * {@link BatchMeteringDecorator#outgoingBatchSizeMetricName},
 * {@link BatchMeteringDecorator#outgoingElementsCountMetricName}.
 *
 * O - type of elements in original batch.
 * R - type of elements in resulting batch.
 * C - type of translation context.
 */
class BatchMeteringDecorator<O, R, C> implements BatchTranslator<O, R, C> {

    /**
     * Translator that is being decorated.
     */
    @Nonnull
    BatchTranslator<O, R, C> next

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
     * Name of the metric that will report how many batches have been received.
     *
     * The closure should take no parameters and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.batches.count.
     */
    @Nonnull
    Closure<String> incomingBatchCountMetricName = { metricsBaseName + '.batches.count' }

    /**
     * Name of the metric that will report sizes of incoming batches.
     *
     * The closure should take no parameters and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.in.batch_size.
     */
    @Nonnull
    Closure<String> incomingBatchSizeMetricName = { metricsBaseName + '.in.batch_size' }

    /**
     * Name of the metric that will report total number of incoming elements.
     *
     * The closure should take no parameters and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.in.events.
     */
    @Nonnull
    Closure<String> incomingElementsCountMetricName = { metricsBaseName + '.in.events' }

    /**
     * Name of the metric that will report time it takes to translate a batch.
     *
     * The closure should take no parameters and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.translate_batch.
     */
    @Nonnull
    Closure<String> translationTimeMetricName = { metricsBaseName + '.translate_batch' }

    /**
     * Name of the metric that will report sizes of resulting batches.
     *
     * The closure should take no parameters and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.out.batch_size.
     */
    @Nonnull
    Closure<String> outgoingBatchSizeMetricName = { metricsBaseName + '.out.batch_size' }

    /**
     * Name of the metric that will report total number of resulting elements.
     *
     * The closure should take no parameters and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.out.events.
     */
    @Nonnull
    Closure<String> outgoingElementsCountMetricName = { metricsBaseName + '.out.events' }

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param metricRegistry Registry where to publish metrics.
     * @param metricsBaseName Base name (prefix) of the metrics.
     */
    BatchMeteringDecorator(@Nonnull BatchTranslator<O, R, C> next,
                           @Nonnull MetricRegistry metricRegistry,
                           @Nonnull String metricsBaseName) {
        this.next = next
        this.metricRegistry = metricRegistry
        this.metricsBaseName = metricsBaseName
    }

    @Override
    List<R> translateBatch(@Nullable List<O> elements, @Nullable C context) {
        int incomingBatchSize = elements ? elements.size() : 0
        metricRegistry.meter(incomingBatchCountMetricName()).mark()
        metricRegistry.histogram(incomingBatchSizeMetricName()).update(incomingBatchSize)
        metricRegistry.meter(incomingElementsCountMetricName()).mark(incomingBatchSize)
        List<R> result = metricRegistry.timer(translationTimeMetricName()).time(
                { next.translateBatch(elements, context) } as Callable<List<R>>)
        int outgoingBatchSize = result ? result.size() : 0
        metricRegistry.histogram(outgoingBatchSizeMetricName()).update(outgoingBatchSize)
        metricRegistry.meter(outgoingElementsCountMetricName()).mark(outgoingBatchSize)
        result
    }
}
