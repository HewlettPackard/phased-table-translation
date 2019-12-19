package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.util.concurrent.Callable

/**
 * Decorates another {@link AroundElement} to report how long it took to process individual element on each stage.
 *
 * The time is reported per element per stage rather than per batch per stage.
 *
 * Metrics will be published in {@link ElementMeteringDecorator#metricRegistry} with
 * {@link ElementMeteringDecorator#metricsBaseName} being the prefix for the name of
 * all metrics.
 *
 * It is possible to override default metric names via
 * {@link ElementMeteringDecorator#timerName}.
 *
 * C - type of translation context.
 */
@CompileStatic
class ElementMeteringDecorator<C> implements AroundElement<C> {

    /**
     * Translator to be decorated.
     */
    @Nonnull
    AroundElement<C> next

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
     * Name of the metric that will report time it took to process single element on each stage.
     *
     * The closure should take stage name as parameter and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}STAGENAME.one.
     */
    @Nonnull
    Closure<String> timerName = { String stageName -> "$metricsBaseName${stageName}.one".toString() }

    /**
     * Creates new instance.
     * @param next Translator to decorate.
     * @param metricRegistry Registry where to report metrics.
     * @param metricsBaseName Prefix for metric names.
     */
    ElementMeteringDecorator(@Nonnull AroundElement<C> next, @Nonnull MetricRegistry metricRegistry,
                             @Nonnull String metricsBaseName) {
        this.next = next
        this.metricRegistry = metricRegistry
        this.metricsBaseName = metricsBaseName
    }

    @Override
    List<?> translateElement(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode, @Nullable Object element,
                             @Nullable C context) {
        metricRegistry.timer(timerName(stageName)).time({
            next.translateElement(stageName, stageCode, element, context)
        } as Callable)
    }
}
