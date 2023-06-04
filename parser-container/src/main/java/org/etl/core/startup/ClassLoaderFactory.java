package org.etl.core.startup;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.etl.core.Repository;

import java.io.File;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class ClassLoaderFactory {





    /**
     * Create and return a new class loader, based on the configuration
     * defaults and the specified directory paths:
     *
     * @param unpacked Array of pathnames to unpacked directories that should
     *  be added to the repositories of the class loader, or <code>null</code>
     * for no unpacked directories to be considered
     * @param packed Array of pathnames to directories containing JAR files
     *  that should be added to the repositories of the class loader,
     * or <code>null</code> for no directories of JAR files to be considered
     * @param parent Parent class loader for the new class loader, or
     *  <code>null</code> for the system class loader.
     * @return the new class loader
     *
     * @exception Exception if an error occurs constructing the class loader
     */
    public static ClassLoader createClassLoader(@Nullable File[] unpacked,
                                                @Nullable File[] packed,
                                                @Nullable final ClassLoader parent)
            throws Exception {

        // Construct the "class path" for this class loader
        Set<URL> set = new LinkedHashSet<>();

        // Add unpacked directories
        if (unpacked != null) {
            for (File file : unpacked) {
                if (!file.canRead()) {
                    log.warn("directory {} cannot be read",file);
                    continue;
                }
                file = new File(file.getCanonicalPath());
                URL url = file.toURI().toURL();
                if (log.isDebugEnabled()) {
                    log.debug("  Including directory " + url);
                }
                set.add(url);
            }
        }

        // Add packed directory JAR files
        if (packed != null) {
            for (File directory : packed) {
                if (!directory.isDirectory() || !directory.canRead()) {
                    log.warn("directory: {} cannot be read",directory);
                    continue;
                }
                String[] filenames = directory.list();
                if (filenames == null) {
                    continue;
                }
                for (String s : filenames) {
                    String filename = s.toLowerCase(Locale.ENGLISH);
                    if (!filename.endsWith(".jar")) {
                        continue;
                    }
                    File file = new File(directory, s);
                    if (log.isDebugEnabled()) {
                        log.debug("  Including jar file " + file.getAbsolutePath());
                    }
                    URL url = file.toURI().toURL();
                    set.add(url);
                }
            }
        }

        // Construct the class loader itself
        final URL[] array = set.toArray(new URL[0]);
        if (parent == null) {
            return new URLClassLoader(array);
        } else {
            return new URLClassLoader(array, parent);
        }
    }


    /**
     * Create and return a new class loader, based on the configuration
     * defaults and the specified directory paths:
     *
     * @param repositories List of class directories, jar files, jar directories
     *                     or URLS that should be added to the repositories of
     *                     the class loader.
     * @param parent Parent class loader for the new class loader, or
     *  <code>null</code> for the system class loader.
     * @return the new class loader
     *
     * @exception Exception if an error occurs constructing the class loader
     */
    public static ClassLoader createClassLoader(List<Repository> repositories,
                                                final ClassLoader parent)
            throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Creating new class loader");
        }

        // Construct the "class path" for this class loader
        Set<URL> set = new LinkedHashSet<>();

        if (repositories != null) {
            for (Repository repository : repositories)  {
                if (repository.getType() == Repository.RepositoryType.URL) {
                    URL url = buildClassLoaderUrl(repository.getLocation());
                    if (log.isDebugEnabled()) {
                        log.debug("  Including URL " + url);
                    }
                    set.add(url);
                } else if (repository.getType() == Repository.RepositoryType.DIR) {
                    File directory = new File(repository.getLocation());
                    directory = directory.getCanonicalFile();
                    if (!validateFile(directory, Repository.RepositoryType.DIR)) {
                        continue;
                    }
                    URL url = buildClassLoaderUrl(directory);
                    if (log.isDebugEnabled()) {
                        log.debug("  Including directory " + url);
                    }
                    set.add(url);
                } else if (repository.getType() == Repository.RepositoryType.JAR) {
                    File file=new File(repository.getLocation());
                    file = file.getCanonicalFile();
                    if (!validateFile(file, Repository.RepositoryType.JAR)) {
                        continue;
                    }
                    URL url = buildClassLoaderUrl(file);
                    if (log.isDebugEnabled()) {
                        log.debug("  Including jar file " + url);
                    }
                    set.add(url);
                } else if (repository.getType() == Repository.RepositoryType.GLOB) {
                    File directory=new File(repository.getLocation());
                    directory = directory.getCanonicalFile();
                    if (!validateFile(directory, Repository.RepositoryType.GLOB)) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("  Including directory glob "
                                + directory.getAbsolutePath());
                    }
                    String[] filenames = directory.list();
                    if (filenames == null) {
                        continue;
                    }
                    for (String s : filenames) {
                        String filename = s.toLowerCase(Locale.ENGLISH);
                        if (!filename.endsWith(".jar")) {
                            continue;
                        }
                        File file = new File(directory, s);
                        file = file.getCanonicalFile();
                        if (!validateFile(file, Repository.RepositoryType.JAR)) {
                            continue;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("    Including glob jar file "
                                    + file.getAbsolutePath());
                        }
                        URL url = buildClassLoaderUrl(file);
                        set.add(url);
                    }
                }
            }
        }

        // Construct the class loader itself
        final URL[] array = set.toArray(new URL[0]);
        if (log.isDebugEnabled()) {
            for (int i = 0; i < array.length; i++) {
                log.debug("  location " + i + " is " + array[i]);
            }
        }

        if (parent == null) {
            return new URLClassLoader(array);
        } else {
            return new URLClassLoader(array, parent);
        }
    }

    /**
     *  URLs passed to class loaders may point to directories that contain
     *  JARs. If these URLs are used to construct URLs for resources in a JAR
     *  the URL will be used as is. It is therefore necessary to ensure that
     *  the sequence "!/" is not present in a class loader URL.
     * @param urlString
     * @return
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    private static URL buildClassLoaderUrl(String urlString) throws MalformedURLException, URISyntaxException {
        String result = urlString.replace("!/", "%21/");
        return new URI(result).toURL();
    }

    private static URL buildClassLoaderUrl(File file) throws MalformedURLException, URISyntaxException {
        // Could be a directory or a file
        String fileUrlString = file.toURI().toString().replace("!/", "%21/");
        return new URI(fileUrlString).toURL();
    }

    public static boolean validateFile(File file,
                                        Repository.RepositoryType type) {
        if (Repository.RepositoryType.DIR == type || Repository.RepositoryType.GLOB == type) {
            if (!file.isDirectory() || !file.canRead()) {
                String msg = "Problem with directory [" + file +
                        "], exists: [" + file.exists() +
                        "], isDirectory: [" + file.isDirectory() +
                        "], canRead: [" + file.canRead() + "]";
                log.warn(msg);
                return false;
            }
        } else if (Repository.RepositoryType.JAR == type) {
            if (!file.canRead()) {
                log.warn("Problem with JAR file [" + file +
                        "], exists: [" + file.exists() +
                        "], canRead: [" + file.canRead() + "]");
                return false;
            }
        }
        return true;
    }

}
