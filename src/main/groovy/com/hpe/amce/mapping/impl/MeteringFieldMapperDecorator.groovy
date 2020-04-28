package com.hpe.amce.mapping.impl

import com.codahale.metrics.MetricRegistry
import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * Decorates another {@link FieldMapper} to record its timing.
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
        metricRegistry.timer(metricName).time(new Runnable() {
            @Override
            void run() {
                delegate.mapField(field, mappingContext)
            }
        })
    }

}
