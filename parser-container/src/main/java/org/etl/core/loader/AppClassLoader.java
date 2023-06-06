package org.etl.core.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * each AppClassLoader load a specific app
 */
public class AppClassLoader extends URLClassLoader {
    private final ClassLoader platformClassLoader;
    ClassLoader systemClassLoader;
    private static final String CLASS_FILE_SUFFIX = ".class";

    private String rootPath;
    private String appName;

    private List<String> resources;

    //todo keep resources myself, define classes myself
    //

    public AppClassLoader(String appMountPath,String appName) {
        this(appMountPath,appName,ClassLoader.getPlatformClassLoader());
    }

    public AppClassLoader(String rootPath,String appName,ClassLoader parent)  {
        super(new URL[0],parent);
        File classesDir = new File(rootPath, appName + File.separator + "BOOT-INF" + File.separator + "classes" + File.separator);
        File jarDir = new File(rootPath, appName + File.separator + "BOOT-INF" + File.separator + "lib" + File.separator);
        List<URL> urls = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(jarDir.getAbsolutePath()), "*.jar")) {
            for (Path path : directoryStream) {
                urls.add(path.toUri().toURL());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (URL url : urls) {
            addURL(url);
        }
        try {
            addURL(classesDir.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


        systemClassLoader = ClassLoader.getSystemClassLoader();
        platformClassLoader = ClassLoader.getPlatformClassLoader();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        //*********
        // common lib or the service cannot be created by this classloader,
        // as  both the classes inside of app and the container should be loaded by only one classloader,
        // otherwise, we will encounter class cast exception

        if (name.startsWith("org.etl.service")){
            return systemClassLoader.loadClass(name);
        }

       return super.loadClass(name, resolve);
    }

}
