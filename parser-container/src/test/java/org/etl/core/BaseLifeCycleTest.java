package org.etl.core;

import org.etl.core.exception.LifecycleException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.*;

class BaseLifeCycleTest {


    @Test
    void init() throws LifecycleException {
        BaseLifeCycleTestImpl test = new BaseLifeCycleTestImpl();
        assertSame(test.getState(), LifeCycle.LifeCycleState.NEW);
        test.init();
        assertSame(test.getState(), LifeCycle.LifeCycleState.INITIALIZED);

        test.init();

        assertFalse(test.isAvailable());

        LifecycleException lifecycleException = assertThrows(LifecycleException.class, test::stop);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("STOPPING"));
        assertFalse(test.isAvailable());

        test.destroy();
        assertSame(test.getState(), LifeCycle.LifeCycleState.DESTROYED);
        assertFalse(test.isAvailable());

        lifecycleException = assertThrows(LifecycleException.class, test::init);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("DESTROYED"));
        assertFalse(test.isAvailable());

        test = new BaseLifeCycleTestImpl();
        test.init();
        assertFalse(test.isAvailable());

        test.start();

        assertTrue(test.isAvailable());
        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);


        lifecycleException = assertThrows(LifecycleException.class, test::init);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("STARTED"));
        assertTrue(test.isAvailable());

    }

    @Test
    void start() throws LifecycleException {
        BaseLifeCycleTestImpl test = new BaseLifeCycleTestImpl();
        test.init();
        assertSame(test.getState(), LifeCycle.LifeCycleState.INITIALIZED);
        test.start();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);

        BaseLifeCycleTestImpl finalTest = test;
        LifecycleException lifecycleException = assertThrows(LifecycleException.class, finalTest::init);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("STARTED"));

        test.start();

        lifecycleException = assertThrows(LifecycleException.class, finalTest::destroy);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("STARTED"));


        test.stop();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STOPPED);

        test.start();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);

        test = new BaseLifeCycleTestImpl();
        test.init();
        test.start();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);
        test.stop();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STOPPED);


    }

    @Test
    void stop() throws LifecycleException {

        BaseLifeCycleTestImpl test = new BaseLifeCycleTestImpl();
        test.init();
        assertSame(test.getState(), LifeCycle.LifeCycleState.INITIALIZED);
        test.start();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);

        test.stop();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STOPPED);

        test.stop();

        LifecycleException lifecycleException = assertThrows(LifecycleException.class, test::init);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("STOPPED"));

        test.start();

        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);

        test.stop();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STOPPED);

        test.destroy();
        assertSame(test.getState(), LifeCycle.LifeCycleState.DESTROYED);

    }

    @Test
    void destroy() throws LifecycleException {

        BaseLifeCycleTestImpl test = new BaseLifeCycleTestImpl();
        test.init();
        assertSame(test.getState(), LifeCycle.LifeCycleState.INITIALIZED);
        test.start();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STARTED);

        test.stop();
        assertSame(test.getState(), LifeCycle.LifeCycleState.STOPPED);

        test.destroy();
        assertSame(test.getState(), LifeCycle.LifeCycleState.DESTROYED);

        LifecycleException lifecycleException = assertThrows(LifecycleException.class, test::init);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("DESTROYED"));

        lifecycleException = assertThrows(LifecycleException.class, test::start);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("DESTROYED"));


        lifecycleException = assertThrows(LifecycleException.class, test::stop);
        Assertions.assertTrue(lifecycleException.getMessage().contains("An invalid Lifecycle transition"));
        Assertions.assertTrue(lifecycleException.getMessage().contains("DESTROYED"));

        test.destroy();

    }
}