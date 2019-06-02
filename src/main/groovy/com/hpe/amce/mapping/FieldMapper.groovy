package com.hpe.amce.mapping

import javax.annotation.Nonnull

/**
 * Maps single field from original object to result object.
 *
 * OO - Original object type.
 * RO - Resulting object type.
 * P - Type of parameters object.
 */
interface FieldMapper<OO, RO, P> {

    /**
     * Applies processing to specified field.
     *
     * @param field Field descriptor telling how to extract, map and inject field value
     * from original to resulting object.
     * @param mappingContext Translation context containing original and result objects.
     */
    void mapField(
            @Nonnull Field<OO, RO, ?, ?, P> field,
            @Nonnull MappingContext<OO, RO, P> mappingContext)

}
