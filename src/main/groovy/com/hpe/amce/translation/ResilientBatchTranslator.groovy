package com.hpe.amce.translation

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j2
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.annotation.PostConstruct
import java.util.concurrent.Callable

/**
 * Translator that resiliently translates batches and supports staged processing.
 *
 * Stages can be used to implement pre and post processing, filtering, etc.
 * Stages are defined using {@link ResilientBatchTranslator#processingStages}.
 *
 * If stage results in an exception then {@link ResilientBatchTranslator#onError}
 * is used to produce result but processing continues with next element.
 *
 * Result of every stage is traced using {@link ResilientBatchTranslator#traceStage}
 * and logger from {@link ResilientBatchTranslator#loggers}.
 *
 * Any null elements and lists are ignored.
 *
 * C - Type of context object.
 *
 * @deprecated Use {@link com.hpe.amce.translation.impl.StagesCaller},
 * {@link com.hpe.amce.translation.impl.ActualStageProcessor}
 * and {@link com.hpe.amce.translation.impl.StageCaller} with desired decorators instead.
 */
@Deprecated
@Log4j2
@CompileStatic
class ResilientBatchTranslator<C> {

    /**
     * Level at which to trace processing.
     *
     * Defaults to {@link org.apache.logging.log4j.Level#TRACE}.
     */
    @Nonnull
    Level traceLevel = Level.TRACE

    /**
     * Logger to be used to trace incoming batch of events.
     *
     * This is usually initialized in {@link #lookupLoggers()}.
     */
    @Nonnull
    Logger loggerIn

    /**
     * Metric registry to which metrics should be reported.
     */
    @Nonnull
    MetricRegistry metricRegistry

    /**
     * Name of this translator.
     */
    @Nonnull
    String name

    /**
     * Dumps list of events.
     *
     * The closure should take two parameters:
     * <ol>
     *     <li>List<?> events - a batch of events to be dumped</li>
     *     <li>StringBuilder buffer - buffer where to dump events to</li>
     * </ol>
     *
     * By default, each event will be placed on a new line.
     *
     * Individual events are dumped using {@link #dumpEventToBuffer}.
     */
    @Nonnull
    Closure dumpBatchToBuffer = { List<?> events, StringBuilder buffer ->
        if (events == null) {
            buffer << 'null'
            return
        }
        buffer << '['
        events.eachWithIndex { Object event, int index ->
            buffer << System.lineSeparator() << '|- ' << index << ' '
            dumpEventToBuffer(event, buffer)
        }
        if (!events.empty) {
            buffer << '|- end of batch'
        }
        buffer << ']'
    }

    /**
     * Dumps event.
     *
     * The closure should take two parameters:
     * <ol>
     *     <li>Object event - event to be dumped (could be null)</li>
     *     <li>StringBuilder buffer - buffer into which to dump event</li>
     * </ol>
     *
     * By default, uses {@link StringBuilder#append(java.lang.Object)} which
     * will call {@link Object#toString()} in most cases.
     */
    @Nonnull
    Closure dumpEventToBuffer = { Object event, StringBuilder buffer ->
        buffer << event
    }

    /**
     * Logs events after being processed in specified stage.
     *
     * The closure should take five parameters:
     * <ol>
     *     <li>StringBuilder buffer - re-usable buffer of high capacity (could need to be cleaned first)</li>
     *     <li>Logger logger - logger to be used for logging</li>
     *     <li>String stageName - name of the processing stage</li>
     *     <li>List<?> events - batch of events after a processing stage</li>
     *     <li>C context - extra parameters that were passed to translator</li>
     * </ol>
     *
     * By default, checks if logger is enabled at {@link #traceLevel} and dumps events
     * using {@link #dumpBatchToBuffer} clearing buffer first.
     */
    @Nonnull
    Closure traceStage = { StringBuilder buffer, Logger logger, String stageName, List<?> events, Object context ->
        if (logger.isEnabled(traceLevel)) {
            if (buffer.length() > 0) {
                buffer.delete(0, buffer.length())
            }
            buffer << 'After ' << stageName << ' with context ' << context << ': '
            dumpBatchToBuffer(events, buffer)
            logger.log(traceLevel, buffer.toString())
        }
    }

    /**
     * Closure that's called on event processing error.
     *
     * The closure should take 4 parameters:
     * <ol>
     *     <li>C context - extra parameters that were passed to translator</li>
     *     <li>String stageName - name of processing stage</li>
     *     <li>Object event - event that has caused error</li>
     *     <li>Exception e - exception that has happened</li>
     * </ol>
     *
     * The closure should return a list of events that should be returned
     * instead of what should have been returned if there was no event.
     *
     * By default, error is logged, error metric for the particular phase is incremented
     * and an empty list is returned.
     *
     * You can overwrite this closure with another one to return something in case
     * of error and call the original closure.
     */
    @Nonnull
    Closure<List<?>> onError = { Object context, String stageName, Object event, Throwable e ->
        metricRegistry.meter("${name}.error.${stageName}").mark()
        log.error("$name $stageName has failed." +
                " Reason: ${e.message}." +
                " Context: $context" +
                " On: ~~~>$event<~~~."
                , e)
        []
    }

    /**
     * Processing stages.
     * <br/>
     * Keys are stage names and entries are closures that do the processing.
     * <br/>
     * The closures should take two parameters.
     * Where first parameter is a single event being processed.
     * Its type should be a type of raw event for the first stage.
     * For the second and further stages, its type should be the same
     * as output of previous stage. For example, if previous stage
     * is just a filter on raw events then for the current stage,
     * the first parameter should also be of raw event type.
     * Second parameter has type C and represents context (extra parameters) passed to
     * {@link #translateBatch}.
     * <br/>
     * The closure should return list of processed events (zero if filtered out,
     * exactly one for one-to-one translation, more than one if any extra
     * events are to be injected).
     * <br/>
     * Stages are called in whatever order map iterates them so use ordered maps.
     */
    @Nonnull
    Map<String, Closure<List<?>>> processingStages

    /**
     * Loggers to be used on each processing stage.
     *
     * Keys are stage names as in {@link #processingStages} and
     * values are loggers to be used on each stage.
     *
     * By default, {@link #lookupLoggers()} will initialize this
     * so that each stage gets logger name that corresponds to
     * stage name.
     */
    @Nonnull
    Map<String, Logger> loggers

    /**
     * Estimated size of text of event dump.
     *
     * This is used to create buffer of enough capacity to avoid re-sizing
     * during event tracing.
     *
     * Default is 10K.
     */
    int estimatedDumpedEventSide = 10 * 1024

    /**
     * Looks up loggers to be used.
     *
     * Uses {@link #name} of this translator and adds to it stage name to
     * construct a logger for specific stage.
     */
    @PostConstruct
    void lookupLoggers() {
        assert processingStages
        loggers = processingStages.collectEntries { String stageName, Closure<?> code ->
            [stageName, LogManager.getLogger("${name}.${stageName}".toString()),]
        } as Map<String, Logger>
        loggerIn = LogManager.getLogger("${name}.input".toString())
    }

    /**
     * Translates a batch of elements.
     * @param events Elements to translate.
     * @param context Extra parameters to pass to pre-, post-processor and translator (optional, can be null).
     * @return Translated elements.
     * @see #processingStages
     */
    @Nonnull
    List<?> translateBatch(@Nullable List<?> events, @Nullable C context = null) {
        assert loggers
        assert processingStages
        int eventsCount = events ? events.size() : 0
        metricRegistry.meter("${name}.batches").mark()
        metricRegistry.histogram("${name}.in.batch_size").update(eventsCount)
        metricRegistry.meter("${name}.in.events").mark(eventsCount)
        List<?> result = metricRegistry.timer("${name}.translate_batch").time(
                new BatchProcessor(makeSafeList(events), context))
        int outCount = result ? result.size() : 0
        metricRegistry.histogram("${name}.out.batch_size").update(outCount)
        metricRegistry.meter("${name}.out.events").mark(outCount)
        result
    }

    /**
     * Applies all stages to a batch of events.
     *
     * Before starting any processing, the batch is traced using
     * {@link ResilientBatchTranslator#traceStage}.
     *
     * Then the batch is processed using every stage in {@link ResilientBatchTranslator#processingStages}.
     *
     * At every stage, each event from a batch is processed in a safe manner
     * so that if an exception is generated for one event then other events will
     * still be processed.
     *
     * A metric will be recorded for the time it took to process
     * every event and every stage (separate metric for every stage).
     */
    private class BatchProcessor implements Callable<List<?>> {
        List events
        Object context

        /**
         * Creates instance.
         * @param events Events to be processed.
         * @param context Context passed to translator.
         */
        BatchProcessor(List events, Object context) {
            this.events = events
            this.context = context
        }

        @Override
        @CompileDynamic
        List<?> call() throws Exception {
            StringBuilder buffer = new StringBuilder(estimatedDumpedEventSide * (1 + events.size()))
            traceStage(buffer, loggerIn, 'input', events, context)
            processingStages.each { String stageName, Closure<List<?>> stageCode ->
                Timer eventTimer = metricRegistry.timer("${name}.one.${stageName}")
                events = events.collectMany { Object event ->
                    eventTimer.time(new SafeEventProcessor(event, context, stageName, stageCode))
                }
                traceStage(buffer, loggers[stageName], stageName, events, context)
            }
            this.events
        }
    }

    /**
     * Applies stage to an event safely.
     *
     * Exceptions generated by stage are intercepted and passed to
     * {@link ResilientBatchTranslator#onError}. Its exceptions
     * are intercepted as well, logged and then an empty list will be returned as result.
     *
     * Any null elements in result are removed.
     * null result list is replaced with empty list.
     */
    private class SafeEventProcessor implements Callable<List<?>> {

        @Nullable
        Object event

        @Nullable
        Object context

        @Nonnull
        String stageName

        @Nonnull
        Closure<List<?>> stageCode

        /**
         * Creates instance.
         * @param event Event to process.
         * @param context Context passed to translator.
         * @param stageName Name of processing stage (for logging).
         * @param stageCode Code of processing stage.
         */
        SafeEventProcessor(
                @Nullable Object event,
                @Nullable Object context,
                @Nonnull String stageName,
                @Nonnull Closure<List<?>> stageCode) {
            this.event = event
            this.context = context
            this.stageName = stageName
            this.stageCode = stageCode
        }

        @Override
        List<?> call() throws Exception {
            processEventSafely(event, context, stageName, stageCode)
        }
    }

    /**
     * Applies stage to event safely.
     *
     * If stageCode generates exception then {@link ResilientBatchTranslator#onError}
     * will be called and its result will be returned instead.
     *
     * If {@link ResilientBatchTranslator#onError} also generates exception
     * then an empty list will be returned.
     *
     * The result is made null-safe: is never null and any null elements removed.
     *
     * @param event Event to process.
     * @param context Context passed to translator.
     * @param stageName Name of processing stage (for logging).
     * @param stageCode Code of processing stage.
     * @return Result of applying stageCode to event and context.
     */
    @Nonnull
    // Want to catch Groovy assertion violations
    @SuppressWarnings('CatchThrowable')
    List<?> processEventSafely(@Nullable Object event,
                               @Nullable Object context,
                               @Nonnull String stageName,
                               @Nonnull Closure<List<?>> stageCode) {
        try {
            makeSafeList(stageCode.call(event, context))
        } catch (Throwable e) {
            try {
                makeSafeList(onError(context, stageName, event, e))
            } catch (Throwable ee) {
                log.error("Failed to process error $e " +
                        "that has been observed for event $event " +
                        "with context $context during stage $stageName", ee)
                []
            }
        }
    }

    /**
     * Gets safer list.
     * @param list Source list.
     * @return Empty list of source is null. Otherwise, same as source but without null elements.
     */
    @Nonnull
    private List<?> makeSafeList(@Nullable List<?> list) {
        (list != null ? list : []).findAll { it != null }
    }

}
