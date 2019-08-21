package com.hpe.amce.translation.impl

import com.hpe.amce.translation.BatchTranslator
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Decorates another {@link BatchTranslator} to trace incoming and resulting batches to Log4J2.
 *
 * Level for original and resulting batches can be controlled via
 * {@link BatchTracingDecorator#inLevel} and {@link BatchTracingDecorator#outLevel}.
 *
 * Logger for original and resulting batches can be controlled via
 * {@link BatchTracingDecorator#inLogger} and {@link BatchTracingDecorator#outLogger}.
 *
 * How original and resulting batches are dumped is controlled via
 * {@link BatchTracingDecorator#inDumper} and {@link BatchTracingDecorator#outDumper}.
 *
 * A batch will be dumped only if corresponding log level is enabled for corresponding logger.
 *
 * O - type of elements in original batch.
 * R - type of elements in resulting batch.
 * C - type of translation context.
 */
class BatchTracingDecorator<O, R, C> implements BatchTranslator<O, R, C> {

    /**
     * Translator that is being decorated.
     */
    @Nonnull
    BatchTranslator<O, R, C> next

    /**
     * Dumper for original batch.
     */
    @Nonnull
    BatchDumper<O, C> inDumper

    /**
     * Dumper for resulting batch.
     */
    @Nonnull
    BatchDumper<R, C> outDumper

    /**
     * Log level for original batch.
     *
     * This is TRACE by default.
     */
    @Nonnull
    Level inLevel = Level.TRACE

    /**
     * Log level for resulting batch.
     *
     * This is TRACE by default.
     */
    @Nonnull
    Level outLevel = Level.TRACE

    /**
     * Logger for original batch.
     *
     * This is {@link BatchTracingDecorator} by default.
     */
    @Nonnull
    Logger inLogger = LogManager.getLogger(BatchTracingDecorator)

    /**
     * Logger for resulting batch.
     *
     * This is {@link BatchTracingDecorator} by default.
     */
    @Nonnull
    Logger outLogger = LogManager.getLogger(BatchTracingDecorator)

    /**
     * Creates new instance.
     * @param next Translator to be decorated.
     * @param inDumper Dumper for original batch.
     * @param outDumper Dumper for resulting batch.
     */
    BatchTracingDecorator(
            @Nonnull BatchTranslator<O, R, C> next,
            @Nonnull BatchDumper<O, C> inDumper,
            @Nonnull BatchDumper<R, C> outDumper) {
        this.next = next
        this.inDumper = inDumper
        this.outDumper = outDumper
    }

    @Override
    List<R> translateBatch(@Nullable List<O> elements, @Nullable C context) {
        if (inLogger.isEnabled(inLevel)) {
            inLogger.log(inLevel, inDumper.dumpBatch(elements, context))
        }
        List<R> result = next.translateBatch(elements, context)
        if (outLogger.isEnabled(outLevel)) {
            outLogger.log(outLevel, outDumper.dumpBatch(result, context))
        }
        result
    }
}
