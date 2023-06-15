package org.etl.core.startup;

import org.etl.core.AppWrapper;
import org.etl.core.BaseLifeCycle;
import org.etl.core.exception.LifecycleException;
import org.etl.core.loader.Loader;
import org.etl.service.Application;
import org.etl.service.Context;

public class TestAppWrapper extends BaseLifeCycle implements AppWrapper {

    private final int reloadTimeInterval;
    protected int reloadTimes = 0;

    public TestAppWrapper(int reloadTimeInterval) {
        this.reloadTimeInterval = reloadTimeInterval;
    }

    @Override
    public String getName() {
        return "testApp";
    }

    @Override
    public boolean getReloadable() {
        return true;
    }

    @Override
    public String getAbsAppPath() {
        return "";
    }

    @Override
    public String getAppMountPath() {
        return "";
    }

    @Override
    public Application getApplicationInstance() {
        return null;
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public void setContext(Context context) {

    }

    @Override
    public void reload() {
        try {
            stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
        System.out.println("TestAppWrapper reloading");
        try {
            Thread.sleep(reloadTimeInterval);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
        reloadTimes++;
    }

    @Override
    public void setLoader(Loader loader) {

    }

    @Override
    public Loader getLoader() {
        return null;
    }

    @Override
    protected void internalInit() throws LifecycleException {
        System.out.println("TestAppWrapper internalInit");
    }

    @Override
    protected void internalStart() throws LifecycleException {
        System.out.println("TestAppWrapper internalStart");

    }

    @Override
    protected void internalStop() throws LifecycleException {
        System.out.println("TestAppWrapper internalStop");
    }

    @Override
    protected void internalDestroy() throws LifecycleException {
        System.out.println("TestAppWrapper internalDestroy");
    }
}
