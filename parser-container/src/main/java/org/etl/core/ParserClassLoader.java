package org.etl.core;

import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ParserClassLoader extends URLClassLoader {
    public static final String CLASS_FILE_SUFFIX = ".class";
    public static final String DEFAULT_ROOT = "parser_root";
    private final ClassLoader parent;
    private Map<String, Class<?>> cached = new HashMap<>();
    URLClassLoader child;
    private ClassLoader javaSeClassLoader;

    protected final Map<String, ResourceEntry> resourceEntries =
            new ConcurrentHashMap<>();

    @Setter
    @Getter
    private boolean delegate = false;
    private String root;


    public ParserClassLoader(URL[] urls) {
        super(new URL[0]);
        ClassLoader p = getParent();
        if (p == null) {
            p = getSystemClassLoader();
        }
        this.parent = p;
        ClassLoader cl = String.class.getClassLoader();
        if (cl == null)
        {
            cl = getSystemClassLoader();
            while (cl.getParent() != null) {
                cl = cl.getParent();
            }
        }

        javaSeClassLoader = cl;
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


    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {

//        checkStateForClassLoading(name);

        // Ask our superclass to locate this class, if possible
        // (throws ClassNotFoundException if it is not found)
        Class<?> clazz = findClassInternal(name);

            if (clazz == null) {
                throw new ClassNotFoundException(name);
            }

        return clazz;
    }


    protected Class<?> findClassInternal(String name) {
        if (name == null) {
            return null;
        }

        String path = binaryNameToPath(name, true);
        ResourceEntry entry = resourceEntries.get(path);
        if (entry == null) {
            entry = new ResourceEntry();
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

            clazz = entry.loadedClass;
            if (clazz != null) {
                return clazz;
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
                    definePackage(packageName, null, null, null, null, null, null, null);
                    pkg = getPackage(packageName);
                }
            }
//        JarFile jarFile = null;
//
//        try {
//            jarFile = getArchiveResourceSet().openJarFile();
//            // Need to create a new JarEntry so the certificates can be read
//            JarEntry jarEntry = jarFile.getJarEntry(getResource().getName());
//            InputStream is = jarFile.getInputStream(jarEntry);
//        }catch (IOException e) {
//
//            if (jarFile != null) {
//                getArchiveResourceSet().closeJarFile();
//            }
//            return null;
//        }
//
//        try {
//            clazz = defineClass(name, binaryContent, 0,
//                    binaryContent.length, new CodeSource(null, certificates));
//        } catch (UnsupportedClassVersionError ucve) {
//            throw new UnsupportedClassVersionError(
//                    ucve.getLocalizedMessage() + " " +
//                            sm.getString("webappClassLoader.wrongVersion",
//                                    name));
//        }
            entry.loadedClass = clazz;

        return clazz;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        ClassLoader javaseLoader = javaSeClassLoader;
        boolean tryLoadingFromJavaseLoader;
        try {
            // Use getResource as it won't trigger an expensive
            // ClassNotFoundException if the resource is not available from
            // the Java SE class loader.
            // See https://bz.apache.org/bugzilla/show_bug.cgi?id=61424 for
            // details of how this may trigger a StackOverflowError
            // Given these reported errors, catch Throwable to ensure all
            // edge cases are also caught
            String resourceName = binaryNameToPath(name, false);
            URL url = javaseLoader.getResource(resourceName);
            tryLoadingFromJavaseLoader = (url != null);
        } catch (Throwable t) {
            // Swallow all exceptions apart from those that must be re-thrown
            // The getResource() trick won't work for this class. We have to
            // try loading it directly and accept that we might get a
            // ClassNotFoundException.
            tryLoadingFromJavaseLoader = true;
        }
        Class<?> clazz;

        if (tryLoadingFromJavaseLoader) {
            try {
                clazz = javaseLoader.loadClass(name);
                if (clazz != null) {
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        //don't load classes that we dont want it to reload such as server level classes, javax.* etc.
        boolean delegated =  delegate || filter(name,true);
        if (delegated) {
            try {
                clazz = Class.forName(name, false, parent);
                if (clazz != null) {
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        // (2) Search local repositories
        try {
            clazz = findClass(name);
            if (clazz != null) {
                System.out.println("Loading class from local repository");
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        // (3) Delegate to parent unconditionally
        if (!delegated) {
            System.out.println(" Delegating to parent classloader at end: " + parent);
            try {
                clazz = Class.forName(name, false, parent);
                if (clazz != null) {
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        throw new ClassNotFoundException(name);
    }


    protected boolean filter(String name, boolean isClassName) {

        if (name == null) {
            return false;
        }

        char ch;
       if (name.startsWith("javax")) {
            /* 5 == length("javax") */
            if (name.length() == 5) {
                return false;
            }
            ch = name.charAt(5);
            if (isClassName && ch == '.') {
                /* 6 == length("javax.") */
                if (name.startsWith("websocket.", 6)) {
                    return true;
                }
            } else if (!isClassName && ch == '/') {
                /* 6 == length("javax/") */
                if (name.startsWith("websocket/", 6)) {
                    return true;
                }
            }
        }
        return false;
    }

//    private byte[] loadClassFromFile(String fileName) throws ClassNotFoundException {
//        String path = root + File.separatorChar + fileName.replace('.', File.separatorChar) + ".class";
//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
//        if (inputStream == null)
//            throw new ClassNotFoundException("unable to find file with path: " + path);
//        byte[] buffer;
//        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//        int nextValue = 0;
//        try {
//            while ((nextValue = inputStream.read()) != -1) {
//                byteStream.write(nextValue);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        buffer = byteStream.toByteArray();
//        return buffer;
//    }

}
