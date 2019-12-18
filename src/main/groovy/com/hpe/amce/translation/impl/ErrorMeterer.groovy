package com.hpe.amce.translation.impl

import com.codahale.metrics.MetricRegistry

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Publishes metric about errors generated by decorated translator.
 *
 * Each time decorated translator throws an error,
 * this decorator increments a meter.
 *
 * The name of meter to be incremented is determined by
 * {@link ErrorMeterer#getMetricName}.
 *
 * C - type of translation context.
 */
class ErrorMeterer<C> implements AroundElement<C> {

    /**
     * Translator to be decorated.
     */
    @Nonnull
    AroundElement<C> decorated

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
     * Name of the metric that will report errors.
     *
     * The closure should take single String parameter that is stage name causing error and should return metric name.
     *
     * By default, this is {@link #metricsBaseName}.error.stageName.
     * Where stageName is a name of the stage that has caused an error.
     */
    @Nonnull
    Closure<String> getMetricName = { "${metricsBaseName}.error.${it}" }

    /**
     * Creates an instance.
     * @param decorated Translator to be decorated.
     * @param metricRegistry Metric registry where metric should be reported to.
     * @param metricsBaseName Base name (prefix) for metrics.
     */
    ErrorMeterer(
            @Nonnull AroundElement<C> decorated,
            @Nonnull MetricRegistry metricRegistry,
            @Nonnull String metricsBaseName) {
        this.decorated = decorated
        this.metricRegistry = metricRegistry
        this.metricsBaseName = metricsBaseName
    }

    @Override
    // Want to catch Groovy assertion violations
    @SuppressWarnings('CatchThrowable')
    List<?> translateElement(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                             @Nullable Object element, @Nullable C context) {
        try {
            decorated.translateElement(stageName, stageCode, element, context)
        } catch (Throwable e) {
            metricRegistry.meter(getMetricName(stageName)).mark()
            throw e
        }
    }
}