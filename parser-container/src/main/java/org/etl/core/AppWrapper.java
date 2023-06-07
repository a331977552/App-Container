package org.etl.core;

import org.etl.core.loader.Loader;
import org.etl.service.Application;
import org.etl.service.Context;

public interface AppWrapper {

    String getName();
    boolean getReloadable();

    public String getAbsAppPath();

    public String getAppMountPath();


    public Application getApplicationInstance();

    public Context getContext();
    public void setContext(Context context);
    void reload();

    public void setLoader(Loader loader);

    public Loader getLoader();

    void start() throws Exception;
    void stop();

    boolean started();
}
