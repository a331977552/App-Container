package org.etl.core;

import org.etl.ContainerApp;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ConatinerClassLoader extends URLClassLoader {
    private final ClassLoader platformClassLoader;
    ClassLoader systemClassLoader;
    private static final String CLASS_FILE_SUFFIX = ".class";


    public ConatinerClassLoader(URL[] urls) throws MalformedURLException {
        this(urls,ClassLoader.getPlatformClassLoader());
    }
    public ConatinerClassLoader(URL[] urls,ClassLoader parent) throws MalformedURLException {
        super(urls,parent);
        systemClassLoader = ClassLoader.getSystemClassLoader();
        platformClassLoader = ClassLoader.getPlatformClassLoader();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    private String binaryNameToPath(String binaryName, boolean withLeadingSlash) {
        // 1 for leading '/', 6 for ".class"
        StringBuilder path = new StringBuilder(7 + binaryName.length());
        if (withLeadingSlash) {
            path.append('/');
        }
        path.append(binaryName.replace('.', '/'));
        path.append(CLASS_FILE_SUFFIX);
        return path.toString();
    }
}
