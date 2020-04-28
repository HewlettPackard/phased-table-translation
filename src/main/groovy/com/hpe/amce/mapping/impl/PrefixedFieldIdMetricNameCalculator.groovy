package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

import javax.annotation.Nonnull

/**
 * Uses field identifier as metric name.
 *
 * This metric name calculator create a metric for each field allowing
 * to track how much time it takes to map each field.
 */
class PrefixedFieldIdMetricNameCalculator<OO, RO, P>
        implements MeteringFieldMapperDecorator.MetricNameCalculator<OO, RO, P> {

    @Nonnull
    private final String prefix

    /**
     * Creates instance.
     * @param prefix Prefix for metric names.
     */
    PrefixedFieldIdMetricNameCalculator(@Nonnull String prefix) {
        this.prefix = prefix
    }

    @Override
    String calculateMetricName(@Nonnull Field field, @Nonnull MappingContext mappingContext) {
        prefix + field.id
    }

}
