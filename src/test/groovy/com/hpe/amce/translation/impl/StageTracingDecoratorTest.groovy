package com.hpe.amce.translation.impl

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.test.appender.ListAppender
import spock.lang.Shared
import spock.lang.Specification

class StageTracingDecoratorTest extends Specification {

    @Shared
    private ListAppender listAppender = LoggerContext.getContext(false).
            getRootLogger().appenders.get("LIST") as ListAppender

    void setup() {
        listAppender.clear()
    }

    def "default configuration"() {
        given:
        String stageName = 'stage'
        AroundStage next = Mock()
        StageDumper dumper = Mock()
        Closure code = Mock()
        List inputBatch = Mock()
        List outputBatch = Mock()
        Object context = Mock()
        and: "default logger set to default TRACE level"
        LoggerContext loggerContext = LogManager.getContext(false) as LoggerContext
        loggerContext.configuration.addAppender(
                new ListAppender("LIST")
        )
        loggerContext.configuration.addLogger(
                StageTracingDecorator.name,
                LoggerConfig.createLogger(
                        true,
                        Level.TRACE,
                        StageTracingDecorator.name,
                        "false",
                        (
                                loggerContext.configuration.appenders.collect {
                                    AppenderRef.createAppenderRef(it.key, null, null)
                                } + [AppenderRef.createAppenderRef('LIST', null, null)]
                        ) as AppenderRef[],
                        null,
                        loggerContext.configuration,
                        null))
        loggerContext.updateLoggers()
        and: "decorator is created with default configuration"
        StageTracingDecorator instance = new StageTracingDecorator(next, dumper)
        when: "decorator is called"
        List result = instance.applyStage(stageName, code, inputBatch, context)
        then: "decorated translator is called and its result is returned"
        1 * next.applyStage(stageName, code, inputBatch, context) >> outputBatch
        result == outputBatch
        and: "in and out are traced on default level of default logger"
        1 * dumper.dumpBeforeStage(stageName, inputBatch, context) >> 'before'
        1 * dumper.dumpAfterStage(stageName, outputBatch, context) >> 'after'
        listAppender.events
                .findAll { it.loggerName == StageTracingDecorator.name }
                .any { it.level == Level.TRACE && it.message.formattedMessage == 'before' }
        listAppender.events
                .findAll { it.loggerName == StageTracingDecorator.name }
                .any { it.level == Level.TRACE && it.message.formattedMessage == 'after' }
        cleanup:
        loggerContext.configuration.removeLogger(StageTracingDecorator.name)
        loggerContext.updateLoggers()
    }

    def "explicit loggers and trace levels"() {
        given:
        String stageName = 'stage'
        AroundStage next = Mock()
        StageDumper dumper = Mock()
        Closure code = Mock()
        List inputBatch = Mock()
        List outputBatch = Mock()
        Object context = Mock()
        and: "custom logger set to trace everything"
        LoggerContext loggerContext = LogManager.getContext(false) as LoggerContext
        loggerContext.configuration.addAppender(
                new ListAppender("LIST")
        )
        loggerContext.configuration.addLogger(
                'test',
                LoggerConfig.createLogger(
                        true,
                        Level.ALL,
                        'test',
                        "false",
                        (
                                loggerContext.configuration.appenders.collect {
                                    AppenderRef.createAppenderRef(it.key, null, null)
                                } + [AppenderRef.createAppenderRef('LIST', null, null)]
                        ) as AppenderRef[],
                        null,
                        loggerContext.configuration,
                        null))
        loggerContext.updateLoggers()
        and: "decorator is given explicit loggers map and custom log level"
        StageTracingDecorator instance = new StageTracingDecorator(next, dumper)
        instance.loggers = [
                (stageName + '.in') : LogManager.getLogger('test.in'),
                (stageName + '.out'): LogManager.getLogger('test.out'),
        ]
        instance.inLevel = Level.DEBUG
        instance.outLevel = Level.INFO
        when: "decorator is called"
        List result = instance.applyStage(stageName, code, inputBatch, context)
        then: "decorated translator is called and its result is returned"
        1 * next.applyStage(stageName, code, inputBatch, context) >> outputBatch
        result == outputBatch
        and: "in and out are traced to custom loggers on custom level"
        1 * dumper.dumpBeforeStage(stageName, inputBatch, context) >> 'before'
        1 * dumper.dumpAfterStage(stageName, outputBatch, context) >> 'after'
        listAppender.events
                .findAll { it.loggerName == 'test.in' }
                .any { it.level == Level.DEBUG && it.message.formattedMessage == 'before' }
        listAppender.events
                .findAll { it.loggerName == 'test.out' }
                .any { it.level == Level.INFO && it.message.formattedMessage == 'after' }
        cleanup:
        loggerContext.configuration.removeLogger('test')
        loggerContext.updateLoggers()
    }

    def "custom loggers lookup"() {
        given:
        String stageName = 'stage'
        AroundStage next = Mock()
        StageDumper dumper = Mock()
        Closure code = Mock()
        List inputBatch = Mock()
        List outputBatch = Mock()
        Object context = Mock()
        and: "custom logger set to default trace level"
        LoggerContext loggerContext = LogManager.getContext(false) as LoggerContext
        loggerContext.configuration.addAppender(
                new ListAppender("LIST")
        )
        loggerContext.configuration.addLogger(
                'test',
                LoggerConfig.createLogger(
                        true,
                        Level.TRACE,
                        'test',
                        "false",
                        (
                                loggerContext.configuration.appenders.collect {
                                    AppenderRef.createAppenderRef(it.key, null, null)
                                } + [AppenderRef.createAppenderRef('LIST', null, null)]
                        ) as AppenderRef[],
                        null,
                        loggerContext.configuration,
                        null))
        loggerContext.updateLoggers()
        and: "decorator is given custom logger lookup"
        StageTracingDecorator instance = new StageTracingDecorator(next, dumper)
        instance.findLoggerForStageAndMode = { String name, Boolean isIn ->
            LogManager.getLogger("test.$name.$isIn")
        }
        when: "decorator is called"
        List result = instance.applyStage(stageName, code, inputBatch, context)
        then: "decorated translator is called and its result is returned"
        1 * next.applyStage(stageName, code, inputBatch, context) >> outputBatch
        result == outputBatch
        and: "in and out are traced to custom loggers on default level"
        1 * dumper.dumpBeforeStage(stageName, inputBatch, context) >> 'before'
        1 * dumper.dumpAfterStage(stageName, outputBatch, context) >> 'after'
        listAppender.events
                .findAll { it.loggerName == "test.${stageName}.true" }
                .any { it.level == Level.TRACE && it.message.formattedMessage == 'before' }
        listAppender.events
                .findAll { it.loggerName == "test.${stageName}.false" }
                .any { it.level == Level.TRACE && it.message.formattedMessage == 'after' }
        cleanup:
        loggerContext.configuration.removeLogger('test')
        loggerContext.updateLoggers()
    }

    def "don't dump when don't need"() {
        given:
        String stageName = 'stage'
        AroundStage next = Mock()
        StageDumper dumper = Mock()
        Closure code = Mock()
        List inputBatch = Mock()
        List outputBatch = Mock()
        Object context = Mock()
        and: "default logger set to INFO level"
        LoggerContext loggerContext = LogManager.getContext(false) as LoggerContext
        loggerContext.configuration.addAppender(
                new ListAppender("LIST")
        )
        loggerContext.configuration.addLogger(
                StageTracingDecorator.name,
                LoggerConfig.createLogger(
                        true,
                        Level.INFO,
                        StageTracingDecorator.name,
                        "false",
                        (
                                loggerContext.configuration.appenders.collect {
                                    AppenderRef.createAppenderRef(it.key, null, null)
                                } + [AppenderRef.createAppenderRef('LIST', null, null)]
                        ) as AppenderRef[],
                        null,
                        loggerContext.configuration,
                        null))
        loggerContext.updateLoggers()
        and: "decorator is configured to trace in on INFO but out stays on TRACE"
        StageTracingDecorator instance = new StageTracingDecorator(next, dumper)
        instance.inLevel = Level.INFO
        when: "decorator is called"
        List result = instance.applyStage(stageName, code, inputBatch, context)
        then: "decorated translator is called and its result is returned"
        1 * next.applyStage(stageName, code, inputBatch, context) >> outputBatch
        result == outputBatch
        and: "in is dumped but out is not"
        1 * dumper.dumpBeforeStage(stageName, inputBatch, context) >> 'before'
        0 * dumper.dumpAfterStage(_, _, _)
        cleanup:
        loggerContext.configuration.removeLogger(StageTracingDecorator.name)
        loggerContext.updateLoggers()
    }
}
