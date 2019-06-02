package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

/**
 * Message formatter that puts all information on single line.
 */
class SingleLineMesasgeFormatter implements MessageFormatter {

    /**
     * Indicates if the field is mandatory.
     */
    boolean mandatory

    @Override
    String formatMessage(MappingContext<?, ?, ?> mappingContext, Field<?, ?, ?, ?, ?> field, String message) {
        String optionality = mandatory ? 'mandatory' : 'optional'
        "$message for $optionality field ${field.id}." +
                " Parameters: ${mappingContext.parameters}." +
                " On: ~~~>${mappingContext.originalObject}<~~~"
    }

}
