package org.etl.core.resources;

import org.etl.core.AppResource;
import org.etl.core.AppResourceRoot;
import org.etl.core.AppResourceSet;
import org.etl.core.AppWrapper;
import org.etl.core.util.URLUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
//jar:file:/C:/proj/parser/jar/parser.jar!/test.xml
public class StanardAppResourceRoot implements AppResourceRoot {

    private final List<AppResourceSet> classResources = new ArrayList<>();
    private final List<AppResourceSet> jarResources = new ArrayList<>();
    private final List<List<AppResourceSet>> allResources = new ArrayList<>();
    private AppWrapper appWrapper;
    {
        allResources.add(classResources);
        allResources.add(jarResources);
    }

    @Override
    public AppResource getResource(String path) {
        return getResource(path, true);
    }


    protected AppResource getResource(String path, boolean validate) {
        if (validate) {
            path = validate(path);
        }
        return getResourceInternal(path);
    }

    protected final AppResource getResourceInternal(String path) {
        AppResource result = null;
        for (List<AppResourceSet> list : allResources) {
            for (AppResourceSet appResourceSet : list) {
                result = appResourceSet.getResource(path);
                if (result.exists()) {
                    return result;
                }
            }
        }
        // Default is empty resource in main resources
        return result;
    }

    @Override
    public AppResource[] getResources(String path) {
        return getResources(path, false);
    }

    public String[] list(String path) {
        return list(path, true);
    }

    private String[] list(String path, boolean validate) {
        if (validate) {
            path = validate(path);
        }
        // Set because we don't want duplicates
        // LinkedHashSet to retain the order. It is the order of the
        // WebResourceSet that matters, but it is simpler to retain the order
        // over all the JARs.
        HashSet<String> result = new LinkedHashSet<>();
        for (List<AppResourceSet> list : allResources) {
            for (AppResourceSet webResourceSet : list) {
                String[] entries = webResourceSet.list(path);
                result.addAll(Arrays.asList(entries));
            }
        }
        return result.toArray(new String[0]);
    }

    private String validate(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("invalid path : " + path);
        }

        String result;
        if (File.separatorChar == '\\') {
            // On Windows '\\' is a separator so in case a Windows style
            // separator has managed to make it into the path, replace it.
            result = URLUtil.normalize(path, true);
        } else {
            // On UNIX and similar systems, '\\' is a valid file name so do not
            // convert it to '/'
            result = URLUtil.normalize(path, false);
        }
        if (result == null || !result.startsWith("/")) {
            throw new IllegalArgumentException("invalid Path Normal : " + path + " result:" + result);
        }

        return result;
    }



    @Override
    public void addJarResources(AppResourceSet webResourceSet) {
        webResourceSet.setRoot(this);
        jarResources.add(webResourceSet);
    }

    @Override
    public AppResourceSet[] getJarResources() {
        return jarResources.toArray(new AppResourceSet[0]);
    }

    @Override
    public AppWrapper getAppWrapper() {
        return appWrapper;
    }

    @Override
    public void setAppWrapper(AppWrapper appWrapper) {
        this.appWrapper = appWrapper;
    }

    private AppResource[] getResources(String path, boolean useClassLoaderResources) {
        path = validate(path);

        return getResourcesInternal(path, useClassLoaderResources);
    }

    protected AppResource[] getResourcesInternal(String path, boolean useClassLoaderResources) {
        List<AppResource> result = new ArrayList<>();
        for (List<AppResourceSet> list : allResources) {
            for (AppResourceSet webResourceSet : list) {
                if (useClassLoaderResources) {
                    AppResource webResource = webResourceSet.getResource(path);
                    if (webResource.exists()) {
                        result.add(webResource);
                    }
                }
            }
        }
        return result.toArray(new AppResource[0]);
    }

    @Override
    public AppResource[] getClassLoaderResources(String path) {
        return getResources("/BOOT-INF/classes" + path, true);
    }


    @Override
    public void createWebResourceSet(ResourceSetType type, String appRoot, String path) {
        List<AppResourceSet> resourceList;
        AppResourceSet resourceSet;
        switch (type) {
            case LIB_JARS:
                resourceList = jarResources;
                break;
            case CLASSES:
                resourceList = classResources;
                break;
            default:
                throw new IllegalArgumentException("unknow resource set type: "+ type);
        }

        // This implementation assumes that the base for all resources will be a
        // file.
        File file = new File(path);

        if (file.isFile()) {
            if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                resourceSet = new JarResourceSet(this, appRoot, base, internalPath);
            } else {
                resourceSet = new FileResourceSet(this, appRoot, base, internalPath);
            }
        } else if (file.isDirectory()) {
            resourceSet = new DirResourceSet(this, appRoot, base, internalPath);
        } else {
            throw new IllegalArgumentException("unabel to create resource set with file: "+file);
        }

        resourceList.add(resourceSet);
    }



}
