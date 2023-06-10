package org.etl.core;

import org.etl.core.exception.LifecycleException;

public interface LifeCycle {

    void init() throws LifecycleException;

    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    void destroy() throws LifecycleException;
    boolean isAvailable();

    LifeCycleState getState();

    enum LifeCycleState {
        NEW(false),
        INITIALIZING(false),
        INITIALIZED(false),
        STARTING(true),
        STARTED(true),

        STOPPING(false),
        STOPPED(false),
        DESTROYING(false),
        DESTROYED(false),
        FAILED(false);
        private final boolean available;

        LifeCycleState(boolean available){
            this.available = available;
        }

        public boolean isAvailable() {
            return available;
        }

    }

}
