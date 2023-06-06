package org.etl.core;

import org.etl.core.loader.Loader;

public interface AppWrapper {

    String getName();
    boolean getReloadable();

    public String getAbsAppPath();

    public String getAppMountPath();


    void reload() ;

    public void setLoader(Loader loader);

    public Loader getLoader();

    void start() throws Exception;
    void stop();

    boolean started();
}
