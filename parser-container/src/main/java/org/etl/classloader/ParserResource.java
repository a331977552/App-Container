package org.etl.classloader;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class ParserResource {



    public void unzip(JarFile jar, String destStr, String parserName) {
        try {
            File dest = new File(destStr);
            if (!dest.isDirectory()) {
                throw new RuntimeException("root parser dir doesn't exist");
            }
            dest = new File(dest.getCanonicalFile(), parserName);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                log.info("11:  "+name);
                File fileInJar = new File(destStr+File.separator+parserName, jarEntry.getName());
                if (jarEntry.isDirectory()) {
                    createDir(fileInJar);
                }else{
                    createDir(fileInJar.getParentFile());
                    try (InputStream sourceStream = jar.getInputStream(jarEntry);
                           OutputStream destStream = new FileOutputStream(fileInJar)) {
                        destStream.write(sourceStream.readAllBytes());
                    }
                }
            }
//                JarEntry jarFileInWar = file.getJarEntry(parserName);
//

//                createWebResourceSet(ResourceSetType.CLASSES_JAR, "/WEB-INF/classes", dest.toURI().toURL(), "/");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    private void createDir(File fileInJar) {
        boolean exists = fileInJar.exists();
        if (!exists){
            boolean mkdirs = fileInJar.mkdirs();
            if (!mkdirs)
                throw new RuntimeException("unable to create dir: "+ fileInJar.getAbsolutePath());
        }
    }
}
