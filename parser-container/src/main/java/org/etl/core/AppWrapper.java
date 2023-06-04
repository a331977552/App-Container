package org.etl.core;

import org.etl.core.loader.Loader;

public interface AppWrapper {

    String name();
    boolean getReloadable();

    void reload() ;

    public void setLoader(Loader loader);

    public Loader getLoader();

    void start() throws Exception;
    void stop();
}
