package org.etl.core.loader;

import lombok.extern.slf4j.Slf4j;
import org.etl.core.AppWrapper;

import java.io.IOException;

@Slf4j
public class AppLoader implements Loader {

    private AppClassLoader appClassLoader;
    private AppWrapper appWrapper;

    @Override
    public void setAppWrapper(AppWrapper appWrapper) {
        this.appWrapper = appWrapper;
    }

    public void setAppClassLoader(AppClassLoader appClassLoader) {
        this.appClassLoader = appClassLoader;
    }

    @Override
    public AppWrapper getAppWrapper() {
        return appWrapper;
    }


    @Override
    public ClassLoader getClassLoader() {
        return appClassLoader;
    }

    @Override
    public void close() {
        try {
            this.appClassLoader.close();
            log.info("class loader {} has been closed. resource is ready to be released",this.appClassLoader);
        } catch (IOException e) {
            log.error("app class loader encounterred exception while closing",e);
        }
    }


}
