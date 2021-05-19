package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext
import groovy.transform.CompileStatic

import javax.annotation.Nonnull

/**
 * Message formatter that puts all information on single line.
 *
 * Information that is added:
 * <ul>
 *     <li>type of field: optional/mandatory</li>
 *     <li>input object</li>
 *     <li>mapping parameters</li>
 *     <li>field identifier</li>
 *     <li>provided message</li>
 * </ul>
 */
@CompileStatic
class SingleLineMesasgeFormatter implements MessageFormatter {

    /**
     * Indicates if the field is mandatory.
     */
    boolean mandatory

    @Override
    @Nonnull
    String formatMessage(
            @Nonnull MappingContext<?, ?, ?> mappingContext,
            @Nonnull Field<?, ?, ?, ?, ?> field,
            @Nonnull String message) {
        String optionality = mandatory ? 'mandatory' : 'optional'
        "$message for $optionality field ${field.id}." +
                " On: ~~~>${mappingContext.originalObject}<~~~" +
                " Parameters: ${mappingContext.parameters}."
    }

}
