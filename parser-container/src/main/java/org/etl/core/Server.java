package org.etl.core;

import jakarta.annotation.Nullable;

public interface Server {

    /**
     *
     * it will add this app to this server and  start the app
     * @param app
     */
    void addApp(AppWrapper app);
    void start();

    void stop();

    public void setAppMountPath(String appMountPath);

    String getAppMountPath();
    /**
     * it will stop this app and then remove it.
     * @param name app name
     */
    AppWrapper removeApp(String name);

    @Nullable
    AppWrapper getApp(String name);
}
