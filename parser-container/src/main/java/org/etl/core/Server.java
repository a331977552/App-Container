package org.etl.core;

import jakarta.annotation.Nullable;
import org.etl.service.Context;

/**
 * a server represents a container, and it contains a lot of applications( extends Application)
 * all these applications share the same context which offers specific services.
 */
public interface Server {

    void start();

    void stop();

    public void setAppMountPath(String appMountPath);

    String getAppMountPath();
    /**
     * it will stop this app and then remove it.
     * @param name app name
     */
    AppWrapper removeApp(String name);

    Context getContext();


    @Nullable
    AppWrapper getApp(String name);
}
