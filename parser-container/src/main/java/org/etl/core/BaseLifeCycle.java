package org.etl.core;

import lombok.extern.slf4j.Slf4j;
import org.etl.core.exception.LifecycleException;

@Slf4j
public abstract class BaseLifeCycle implements LifeCycle {
    private LifeCycleState state = LifeCycleState.NEW;

    @Override
    public final void init() throws LifecycleException {
        if (state != LifeCycleState.NEW) {
            this.invalidTransition(LifeCycleState.INITIALIZING);
        }
        try {
            setInternalState(LifeCycleState.INITIALIZING);
            internalInit();
            setInternalState(LifeCycleState.INITIALIZED);
        } catch (Throwable throwable) {
            handException(throwable, "failed to init component {}", this);
        }
    }

    private void invalidTransition(LifeCycleState intentedState) throws LifecycleException {
        throw new LifecycleException("An invalid Lifecycle transition was attempted " + intentedState + " for component " + this + " in state " + state);
    }


    protected abstract void internalInit() throws LifecycleException;


    @Override
    public final void start() throws LifecycleException {

        if (state == LifeCycleState.STARTING || state == LifeCycleState.STARTED) {
            log.error("{} component has been started already", this, new LifecycleException());
            return;
        }
        //checked destroy scenario
        if (state != LifeCycleState.INITIALIZED && state != LifeCycleState.STOPPED) {
            this.invalidTransition(LifeCycleState.STARTING);
        }

        try {
            this.setInternalState(LifeCycleState.STARTING);
            internalStart();
            if (state == LifeCycleState.FAILED) {
                this.stop();
            } else if (state != LifeCycleState.STARTING) {
                this.invalidTransition(LifeCycleState.STARTED);
            } else {
                this.setInternalState(LifeCycleState.STARTED);
            }
        } catch (Throwable throwable) {
            handException(throwable, "failed to start component {}", this);
        }

    }

    private void handException(Throwable e, String msg, Object... args) throws LifecycleException {
        log.error(msg, args, e);
        setInternalState(LifeCycleState.FAILED);
//        throw e;
    }

    protected void setInternalState(LifeCycleState state) {
        this.state = state;
    }

    protected abstract void internalStart() throws LifecycleException;

    @Override
    public final void stop() throws LifecycleException {
        if (state == LifeCycleState.STOPPING || state == LifeCycleState.STOPPED) {
            log.error("{} component has been stopped already", this, new LifecycleException());
            return;
        }

        if (state != LifeCycleState.STARTED && state != LifeCycleState.FAILED) {
            this.invalidTransition(LifeCycleState.STOPPING);
        }
        setInternalState(LifeCycleState.STOPPING);
        try {
            internalStop();
            if (state != LifeCycleState.STOPPING && state != LifeCycleState.FAILED) {
                this.invalidTransition(LifeCycleState.STOPPED);
            }
            setInternalState(LifeCycleState.STOPPED);
        } catch (Throwable throwable) {
            handException(throwable, "failed to stop component {}", this);
        }

    }

    protected abstract void internalStop() throws LifecycleException;

    @Override
    public final void destroy() throws LifecycleException {
        if (state == LifeCycleState.DESTROYING || state == LifeCycleState.DESTROYED) {
            log.error("{} component has been destroyed already", this, new LifecycleException());
            return;
        }
        //many states can be destroyed
        if (state != LifeCycleState.STOPPED
                && state != LifeCycleState.INITIALIZED
                && state != LifeCycleState.NEW
                && state != LifeCycleState.FAILED) {
            this.invalidTransition(LifeCycleState.DESTROYING);
        }
        try {
            setInternalState(LifeCycleState.DESTROYING);
            internalDestroy();
            setInternalState(LifeCycleState.DESTROYED);
        } catch (Throwable throwable) {
            handException(throwable, "failed to destroy component {}", this);
        }

    }

    protected abstract void internalDestroy() throws LifecycleException;

}
