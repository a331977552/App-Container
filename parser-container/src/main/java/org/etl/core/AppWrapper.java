package org.etl.core;

import org.etl.core.loader.Loader;
import org.etl.service.Application;
import org.etl.service.Context;

public interface AppWrapper extends LifeCycle,Reloadable {

    String getName();
    boolean getReloadable();

    public String getAbsAppPath();

    public String getAppMountPath();

    public Application getApplicationInstance();

    public Context getContext();
    public void setContext(Context context);

    public void setLoader(Loader loader);

    public Loader getLoader();
}
