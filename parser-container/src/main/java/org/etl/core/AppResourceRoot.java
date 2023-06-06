package org.etl.core;

import java.net.URL;

/**
 * Represents the complete set of resources for an application. The resources
 * for an  application comprise multiple ResourceSets and when looking for
 * a Resource, the ResourceSets are processed in the following order:
 */
public interface AppResourceRoot {
    AppResource getResource(String path);

    AppResource[] getResources(String path);

    /**
     * Obtain the list of the names of all the files and directories located
     * in the specified directory.
     *
     * @param path The path for the resource of interest relative to the root
     *             of the web application. It must start with '/'.
     * @return The list of resources. If path does not refer to a directory
     * then a zero length array will be returned.
     */
    String[] list(String path);



    /**
     * Adds the provided AppResourceSet to this application as a 'Jar'
     * resource.
     *
     * @param webResourceSet the resource set to use
     */
    void addJarResources(AppResourceSet webResourceSet);

    AppResourceSet[] getJarResources();

    AppWrapper getAppWrapper();

    void setAppWrapper(AppWrapper appWrapper);


    AppResource[] getClassLoaderResources(String path);


    void createWebResourceSet(ResourceSetType type, String appRoot, String path);
    enum ResourceSetType {
        LIB_JARS, //BOOT-INF/lib/*
        CLASSES // BOOT-INF/classes/.class
    }
}
