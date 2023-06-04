package org.etl.core;

public interface Server {

    /**
     *
     * it will add this app to this server and  start the app
     * @param app
     */
    void addApp(AppWrapper app);
    void start();

    void stop();


    /**
     * it will stop this app and then remove it
     * @param name app name
     */
    AppWrapper removeApp(String name);
}
