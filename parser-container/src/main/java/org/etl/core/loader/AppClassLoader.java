package org.etl.core.loader;

import org.etl.core.AppResource;
import org.etl.core.ResourceEntry;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

/**
 * each AppClassLoader load a specific app
 */
public class AppClassLoader extends URLClassLoader {
    private final ClassLoader platformClassLoader;
    ClassLoader systemClassLoader;
    private static final String CLASS_FILE_SUFFIX = ".class";

    private String rootPath;
    private String appName;

    protected final Map<String, ResourceEntry> resourceEntries =
            new ConcurrentHashMap<>();

    //todo keep resources myself, define classes myself
    //

    public AppClassLoader(String rootPath,String appName) {
        this(rootPath,appName,ClassLoader.getPlatformClassLoader());
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

    private Class<?> findClassInternal(String name){
//        checkStateForResourceLoading(name);// It is not permitted to load new classes once the web application has been stopped.

        if (name == null) {
            return null;
        }
        String path = binaryNameToPath(name, true);

        ResourceEntry entry = resourceEntries.get(path);
        AppResource resource = null;

        if (entry == null) {
            resource = resources.getClassLoaderResource(path);

            if (!resource.exists()) {
                return null;
            }

            entry = new ResourceEntry();
            entry.lastModified = resource.getLastModified();

            // Add the entry in the local resource repository
            synchronized (resourceEntries) {
                // Ensures that all the threads which may be in a race to load
                // a particular class all end up with the same ResourceEntry
                // instance
                ResourceEntry entry2 = resourceEntries.get(path);
                if (entry2 == null) {
                    resourceEntries.put(path, entry);
                } else {
                    entry = entry2;
                }
            }
        }

        Class<?> clazz = entry.loadedClass;
        if (clazz != null) {
            return clazz;
        }

        synchronized (JreCompat.isGraalAvailable() ? this : getClassLoadingLock(name)) {
            clazz = entry.loadedClass;
            if (clazz != null) {
                return clazz;
            }

            if (resource == null) {
                resource = resources.getClassLoaderResource(path);
            }

            if (!resource.exists()) {
                return null;
            }

            byte[] binaryContent = resource.getContent();
            if (binaryContent == null) {
                // Something went wrong reading the class bytes (and will have
                // been logged at debug level).
                return null;
            }
            Manifest manifest = resource.getManifest();
            Certificate[] certificates = resource.getCertificates();

            if (transformers.size() > 0) {
                // If the resource is a class just being loaded, decorate it
                // with any attached transformers

                // Ignore leading '/' and trailing CLASS_FILE_SUFFIX
                // Should be cheaper than replacing '.' by '/' in class name.
                String internalName = path.substring(1, path.length() - CLASS_FILE_SUFFIX.length());

                for (ClassFileTransformer transformer : this.transformers) {
                    try {
                        byte[] transformed = transformer.transform(
                                this, internalName, null, null, binaryContent);
                        if (transformed != null) {
                            binaryContent = transformed;
                        }
                    } catch (IllegalClassFormatException e) {
                        log.error(sm.getString("webappClassLoader.transformError", name), e);
                        return null;
                    }
                }
            }

            // Looking up the package
            String packageName = null;
            int pos = name.lastIndexOf('.');
            if (pos != -1) {
                packageName = name.substring(0, pos);
            }

            Package pkg = null;

            if (packageName != null) {
                pkg = getPackage(packageName);

                // Define the package (if null)
                if (pkg == null) {
                    try {
                        if (manifest == null) {
                            definePackage(packageName, null, null, null, null, null, null, null);
                        } else {
                            definePackage(packageName, manifest, null);
                        }
                    } catch (IllegalArgumentException e) {
                        // Ignore: normal error due to dual definition of package
                    }
                    pkg = getPackage(packageName);
                }
            }

            try {
                clazz = defineClass(name, binaryContent, 0,
                        binaryContent.length, new CodeSource(null, certificates));
            } catch (UnsupportedClassVersionError ucve) {
                throw new UnsupportedClassVersionError(
                        ucve.getLocalizedMessage() + " " +
                                sm.getString("webappClassLoader.wrongVersion",
                                        name));
            }
            entry.loadedClass = clazz;
        }

        return clazz;
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



    /**
     * Have one or more classes or resources been modified so that a reload
     * is appropriate?
     * @return <code>true</code> if there's been a modification
     */
    public boolean modified() {


        for (Map.Entry<String, ResourceEntry> entry : resourceEntries.entrySet()) {
            long cachedLastModified = entry.getValue().lastModified;
            long lastModified = resources.getClassLoaderResource(
                    entry.getKey()).getLastModified();
            if (lastModified != cachedLastModified) {
                if( log.isDebugEnabled() ) {
                    log.debug(sm.getString("webappClassLoader.resourceModified",
                            entry.getKey(),
                            new Date(cachedLastModified),
                            new Date(lastModified)));
                }
                return true;
            }
        }

        // Check if JARs have been added or removed
        WebResource[] jars = resources.listResources("/WEB-INF/lib");
        // Filter out non-JAR resources

        int jarCount = 0;
        for (WebResource jar : jars) {
            if (jar.getName().endsWith(".jar") && jar.isFile() && jar.canRead()) {
                jarCount++;
                Long recordedLastModified = jarModificationTimes.get(jar.getName());
                if (recordedLastModified == null) {
                    // Jar has been added
                    log.info(sm.getString("webappClassLoader.jarsAdded",
                            resources.getContext().getName()));
                    return true;
                }
                if (recordedLastModified.longValue() != jar.getLastModified()) {
                    // Jar has been changed
                    log.info(sm.getString("webappClassLoader.jarsModified",
                            resources.getContext().getName()));
                    return true;
                }
            }
        }

        if (jarCount < jarModificationTimes.size()){
            log.info(sm.getString("webappClassLoader.jarsRemoved",
                    resources.getContext().getName()));
            return true;
        }


        // No classes have been modified
        return false;
    }
}
