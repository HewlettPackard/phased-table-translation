package com.hpe.amce.mapping.impl.statemachine

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.MappingContext

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 * Base class for steps in state machine for a field translation.
 *
 * Since steps reference each other and can hardly be created without
 * egg-chicken problem, {@link AbstractState#configure} should be used
 * to configure a state once it is created.
 */
abstract class AbstractState implements State {

    private State onException

    private State onNull

    private State onNonNull

    private State onUndefined

    /**
     * Configures this step.
     * @param onNonNull Where to transition if current step returns not null.
     * @param onNull Where to transition if current steps returns null.
     * @param onException Where to transition when exception happens during current step.
     * @param onUndefined Where to transition if current step is not configured.
     * Like when validator is not defined.
     */
    void configure(
            @Nonnull State onNonNull,
            @Nonnull State onNull,
            @Nonnull State onException,
            @Nonnull State onUndefined) {
        assert onNonNull
        assert onNull
        assert onException
        assert onUndefined
        this.onException = onException
        this.onNull = onNull
        this.onNonNull = onNonNull
        this.onUndefined = onUndefined
    }

    /**
     * Is resulting value null?
     * @param value Result of current step evaluation.
     * @return true if should transition to {@link AbstractState#onNull}.
     */
    protected boolean isNull(@Nullable Object value) {
        value == null
    }

    /**
     * Perform an action.
     * @param field Field we're working on.
     * @param mappingContext Mapping context.
     * @param machineContext State of translation machine.
     * @param propagate Should current step propagate it's result as input to next step?
     * @param action Action to perform.
     * @return Result of calling next step on result returned by action.
     * If current step is not configured for a field as per {@link AbstractState#isDefined}
     * then next step is {@link AbstractState#onUndefined}.
     * In case of action throwing exception, next step will be {@link AbstractState#onException}.
     * If action returns null as per {@link AbstractState#isNull} then next step will be {@link AbstractState#onNull}.
     * Otherwise, next step is {@link AbstractState#onNonNull}.
     */
    @Nullable
    // We want to catch assert violations
    @SuppressWarnings('CatchThrowable')
    protected Object safely(
            @Nonnull Field field,
            @Nonnull MappingContext mappingContext,
            @Nonnull MachineContext machineContext,
            boolean propagate,
            @Nonnull Closure action) {
        if (!isDefined(field)) {
            if (onUndefined != null) {
                return onUndefined.process(field, mappingContext, machineContext)
            }
            throw new IllegalStateException("$this is undefined for $field and onUndefined is not set")
        }
        Object result
        try {
            result = action()
        } catch (Throwable e) {
            machineContext.error = e
            return onException.process(field, mappingContext, machineContext)
        }
        if (propagate) {
            machineContext.resultValue = result
        }
        if (isNull(result)) {
            onNull.process(field, mappingContext, machineContext)
        } else {
            onNonNull.process(field, mappingContext, machineContext)
        }
    }

    /**
     * Calls closure with specified delegate.
     *
     * Creates closure clone so this method is safe to use in multithreaded environment.
     *
     * @param closure Closure to call.
     * @param delegate Delegate to use.
     * @param args Closure parameters.
     * @return Whatever closure returns.
     */
    protected <T> T callWithDelegate(
            @Nonnull Closure<T> closure,
            @Nonnull Object delegate,
            @Nonnull Object... args) {
        Closure<T> clonedClosure = closure.clone() as Closure<T>
        clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
        clonedClosure.delegate = delegate
        args == null ?
                clonedClosure.call(null) :
                args.length == 0 ?
                        clonedClosure.call() :
                        args.length == 1 ?
                                clonedClosure.call(args[0]) :
                                clonedClosure.call(args as List)
    }

}
