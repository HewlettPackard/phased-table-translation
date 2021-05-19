package com.hpe.amce.mapping.impl

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * Decorates another {@link FieldMapper} to record its timing.
 *
 * Parameters of metric can be customized via {@link MeteringFieldMapperDecorator#metricTimerFactory}.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
@CompileStatic
class MeteringFieldMapperDecorator<OO, RO, P> implements FieldMapper<OO, RO, P> {

    /**
     * Calculates metric name.
     *
     * OO - Original object type.
     * RO - Resulting object type.
     * P - Type of parameters object.
     */
    interface MetricNameCalculator<OO, RO, P> {
        /**
         * Calculates name of the metric.
         * @param field Field that is being mapped.
         * @param mappingContext Mapping context.
         * @return Name of the metric that should track mapping time of delegate.
         */
        @Nonnull
        String calculateMetricName(@Nonnull Field<OO, RO, ?, ?, P> field,
                                   @Nonnull MappingContext<OO, RO, P> mappingContext)
    }

    @Nonnull
    private final FieldMapper<OO, RO, P> delegate

    @Nonnull
    private final MetricRegistry metricRegistry

    @Nonnull
    private final MetricNameCalculator metricNameCalculator

    /**
     * Factory that is to be used to create timer metrics.
     *
     * By default, uses default parameters of {@link com.codahale.metrics.Timer#Timer()}.
     */
    @Nonnull
    MetricRegistry.MetricSupplier<Timer> metricTimerFactory = new MetricRegistry.MetricSupplier<Timer>() {
        @Override
        Timer newMetric() {
            new Timer()
        }
    }

    /**
     * Creates instance.
     * @param delegate Mapper to be metered.
     * @param metricRegistry Registry where to report metrics to.
     * @param metricNameCalculator Calculator for metric name.
     */
    MeteringFieldMapperDecorator(
            @Nonnull FieldMapper<OO, RO, P> delegate,
            @Nonnull MetricRegistry metricRegistry,
            @Nonnull MetricNameCalculator metricNameCalculator) {
        this.delegate = delegate
        this.metricRegistry = metricRegistry
        this.metricNameCalculator = metricNameCalculator
    }

    @Override
    void mapField(@Nonnull Field<OO, RO, ?, ?, P> field, @Nonnull MappingContext<OO, RO, P> mappingContext) {
        String metricName = metricNameCalculator.calculateMetricName(field, mappingContext)
        metricRegistry.timer(metricName, metricTimerFactory).time(new Runnable() {
            @Override
            void run() {
                delegate.mapField(field, mappingContext)
            }
        })
    }

}
