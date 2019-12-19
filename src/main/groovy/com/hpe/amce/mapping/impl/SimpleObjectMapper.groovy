package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.FieldMapper
import com.hpe.amce.mapping.MappingContext
import com.hpe.amce.mapping.ObjectMapper

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Simple object mapper that applies fields mappers
 * one-by-one in the same thread in the specified order.
 *
 * Mandatory fields are mapped using {@link SimpleObjectMapper#mandatoryFieldMapper}
 * while optional fields are mapped using {@link SimpleObjectMapper#optionalFieldMapper}.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
class SimpleObjectMapper<OO, RO, P> implements ObjectMapper<OO, RO, P> {

    /**
     * Mapper for optional fields.
     *
     * By default, this is {@link OptionalFieldMapper}.
     */
    @Nonnull
    FieldMapper<OO, RO, P> optionalFieldMapper = new OptionalFieldMapper<>()

    /**
     * Mapper for mandatory fields.
     *
     * By default, this is {@link MandatoryFieldMapper}.
     */
    @Nonnull
    FieldMapper<OO, RO, P> mandatoryFieldMapper = new MandatoryFieldMapper<>()

    @Override
    void mapAllFields(
            @Nonnull OO raw,
            @Nonnull RO translated,
            @Nonnull Map<Field<OO, RO, ?, ?, P>, Boolean> fields,
            @Nullable P parameters) {
        assert raw != null
        assert translated != null
        assert fields != null
        MappingContext<OO, RO, P> mappingContext =
                // https://issues.apache.org/jira/browse/GROOVY-7419
                new MappingContext<OO, RO, P>(originalObject: raw, resultObject: translated, parameters: parameters)
        fields.each { field, mandatory ->
            (mandatory ? mandatoryFieldMapper : optionalFieldMapper).mapField(field, mappingContext)
        }
    }
}
