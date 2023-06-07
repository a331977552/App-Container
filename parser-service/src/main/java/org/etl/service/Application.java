package org.etl.service;

import org.etl.service.service.CacheUtil;

public interface Application {

    public void start(Context context);

    public void stop(boolean force);

}
