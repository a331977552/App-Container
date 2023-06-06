package org.etl.core;

import java.net.URL;
import java.util.Set;

public interface AppResourceSet {
    AppResource getResource(String path);

    String[] list(String path);

    boolean mkdir(String path);

    void setRoot(AppResourceRoot root);

    Set<String> listWebAppPaths(String path);

    public URL getBaseUrl();

    void gc();
}
