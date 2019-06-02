package com.hpe.amce.mapping

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Maps original object to resulting object.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
interface ObjectMapper<OO, RO, P> {

    /**
     * Applies translation to all specified fields.
     *
     * @param raw Original object to translate.
     * It will be available as {@link MappingContext#originalObject}.
     * @param translated Resulting object to populate.
     * It will be available as {@link MappingContext#resultObject}.
     * @param fields Translation map. Keys are field descriptors and
     * values specify if field is mandatory (true) or optional (false).
     * @param parameters Additional parameters to be passed to translator.
     * Those will be available as {@link MappingContext#parameters}.
     * @see FieldMapper#mapField
     */
    void mapAllFields(
            @Nonnull OO raw,
            @Nonnull RO translated,
            @Nonnull Map<Field<OO, RO, ?, ?, P>, Boolean> fields,
            @Nullable P parameters)

}
