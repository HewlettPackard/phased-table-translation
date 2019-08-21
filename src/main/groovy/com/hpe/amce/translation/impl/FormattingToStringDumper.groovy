package com.hpe.amce.translation.impl

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Dumps element per line using {@link Object#toString()}.
 *
 * When dumping batches, each element will be placed on a new line.
 * Start of each element will be indicated with bullet and all elements
 * of batch will be numbered for easier identification.
 * This can be controlled via {@link FormattingToStringDumper#dumpBatchToBuffer}.
 *
 * When dumping stages, an arrow near stage name will indicate if stage receives our outputs following
 * data.
 *
 * Individual elements will be converted to strings using their toString
 * method. This can be controlled via
 * {@link FormattingToStringDumper#dumpElementToBuffer}.
 *
 * {@link StringBuilder} is used internally to concatenate all necessary information.
 * {@link FormattingToStringDumper#estimatedDumpedEventSize} can be adjusted
 * to avoid resizing of this buffer.
 */
class FormattingToStringDumper implements BatchDumper<Object, Object>, StageDumper<Object> {

    /**
     * Dumps a batch of elements.
     *
     * The closure should take three parameters:
     * <ol>
     *     <li>List<?> events - a batch of elements to be dumped (could be null)</li>
     *     <li>Object context - Translation context (could be null).
     *     <li>StringBuilder buffer - buffer where to dump elements to</li>
     * </ol>
     *
     * By default, each element will be placed on a new line.
     *
     * Individual elements are dumped using {@link #dumpElementToBuffer}.
     */
    @Nonnull
    Closure dumpBatchToBuffer = { List<?> elements, Object context, StringBuilder buffer ->
        if (elements == null) {
            buffer << 'null batch with context ' << context
            return
        }
        buffer << '['
        elements.eachWithIndex { Object element, int index ->
            buffer << System.lineSeparator() << '|- ' << index << ' '
            dumpElementToBuffer(element, context, buffer)
        }
        if (!elements.empty) {
            buffer << '|- end of batch'
        }
        buffer << System.lineSeparator() << '] with context ' << context
    }

    /**
     * Dumps individual element.
     *
     * The closure should take two parameters:
     * <ol>
     *     <li>Object element - element to be dumped (could be null)</li>
     *     <li>Object context - Translation context (could be null).
     *     <li>StringBuilder buffer - buffer into which to dump event</li>
     * </ol>
     *
     * By default, uses {@link StringBuilder#append(java.lang.Object)} which
     * will call {@link Object#toString()} in most cases.
     */
    @Nonnull
    Closure dumpElementToBuffer = { Object element, Object context, StringBuilder buffer ->
        buffer << element
    }

    /**
     * Estimated size of text of event dump.
     *
     * This is used to create buffer of enough capacity to avoid re-sizing
     * during event tracing.
     *
     * Default is 10K.
     */
    int estimatedDumpedEventSize = 10 * 1024

    @Override
    String dumpBatch(@Nullable List<Object> batch, @Nullable Object context) {
        StringBuilder buffer = makeBufferForBatch(batch)
        dumpBatchToBuffer(batch, context, buffer)
        buffer.toString()
    }

    @Override
    String dumpBeforeStage(@Nonnull String stageName, @Nullable List<?> batch, @Nullable Object context) {
        StringBuilder buffer = makeBufferForBatch(batch)
        buffer << stageName << ' <- '
        dumpBatchToBuffer(batch, context, buffer)
        buffer.toString()
    }

    @Override
    String dumpAfterStage(@Nonnull String stageName, @Nullable List<?> batch, @Nullable Object context) {
        StringBuilder buffer = makeBufferForBatch(batch)
        buffer << stageName << ' -> '
        dumpBatchToBuffer(batch, context, buffer)
        buffer.toString()
    }

    /**
     * Creates a buffer to hold batch dump.
     *
     * By default, will allocate new buffer using batch size
     * and {@link #estimatedDumpedEventSize} to try to avoid resizing.
     *
     * @param batch Batch to be dumped or null if none.
     * @return Buffer where batch should be dumped.
     */
    protected StringBuilder makeBufferForBatch(@Nullable List<?> batch) {
        int bufferSize = batch?.size() ? (batch.size() + 1) * estimatedDumpedEventSize : estimatedDumpedEventSize
        new StringBuilder(bufferSize)
    }
}
