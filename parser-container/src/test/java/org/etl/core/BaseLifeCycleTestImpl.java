package org.etl.core;

import org.etl.core.exception.LifecycleException;
import org.junit.jupiter.api.Assertions;

class BaseLifeCycleTestImpl extends BaseLifeCycle{
    @Override
    protected void internalInit() throws LifecycleException {
        Assertions.assertSame(this.getState(), LifeCycleState.INITIALIZING);
        System.out.println("internalInit");
    }

    @Override
    protected void internalStart() throws LifecycleException {
        Assertions.assertSame(this.getState(), LifeCycleState.STARTING);
        System.out.println("internalStart");
    }

    @Override
    protected void internalStop() throws LifecycleException {
        Assertions.assertSame(this.getState(), LifeCycleState.STOPPING);
        System.out.println("internalStop");
    }

    @Override
    protected void internalDestroy() throws LifecycleException {
        Assertions.assertSame(this.getState(), LifeCycleState.DESTROYING);
        System.out.println("internalDestroy");
    }
}
