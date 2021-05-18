package com.hpe.amce.translation.impl

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Dumps to string batch of translated elements.
 *
 * E - type of elements.
 * C - translation context.
 *
 * @see BatchDumper#dumpBatch
 */
interface BatchDumper<E, C> {

    /**
     * Dumps specified batch of elements to string.
     * @param batch Batch to be dumped or null if not specified.
     * @param context Translation context or null if not specified.
     * @return String representation of batch and/or context.
     */
    @Nonnull
    String dumpBatch(@Nullable List<E> batch, @Nullable C context)

}
