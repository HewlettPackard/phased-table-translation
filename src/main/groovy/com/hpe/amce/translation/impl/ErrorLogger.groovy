package com.hpe.amce.translation.impl

import groovy.util.logging.Log4j2

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Logs error generated by decorated translator.
 *
 * Once error is logged, it will be thrown again
 * so it can be caught to be reported as metric or ignored or propagated.
 *
 * C - type of translation context.
 */
@Log4j2
class ErrorLogger<C> implements AroundElement<C> {

    /**
     * Translator to be decorated.
     */
    @Nonnull
    AroundElement<C> decorated

    /**
     * Creates instance.
     * @param decorated Translator to be decorated.
     */
    ErrorLogger(@Nonnull AroundElement<C> decorated) {
        this.decorated = decorated
    }

    @Override
    // Want to catch Groovy assertion violations
    @SuppressWarnings('CatchThrowable')
    List<?> translateElement(@Nonnull String stageName, @Nonnull Closure<List<?>> stageCode,
                             @Nullable Object element, @Nullable C context) {
        try {
            decorated.translateElement(stageName, stageCode, element, context)
        } catch (Throwable e) {
            log.error("$stageName has failed." +
                    " Reason: ${e.message}." +
                    " Context: $context" +
                    " On: $element"
                    , e)
            throw e
        }
    }
}