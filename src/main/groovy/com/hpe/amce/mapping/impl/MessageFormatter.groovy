package com.hpe.amce.mapping.impl

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

/**
 * Formats messages related to mapping.
 */
interface MessageFormatter {

    /**
     * Creates a message explaining what's going on.
     * @param mandatory true if we were translating mandatory field.
     * @param mappingContext Translation context.
     * @param field Field we were translating.
     * @param message Explanation of what went wrong or what we were doing.
     * @return Detailed message.
     */
    String formatMessage(MappingContext<?, ?, ?> mappingContext, Field<?, ?, ?, ?, ?> field, String message)

}
