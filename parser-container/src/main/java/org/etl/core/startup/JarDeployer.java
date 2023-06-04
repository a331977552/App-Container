package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.etl.core.startup.BootStrap;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
@Component
public class JarDeployer {
    private final String rootPath = BootStrap.APP_HOME;


    public JarDeployer() {
    }

    public File deploy(String serviceName, File jar) {
        log.info("deploy parser jar :{} with name: {}", jar.getAbsolutePath(), serviceName);
        deleteExistingService(rootPath + File.separator + serviceName);
        return unzip(jar.getAbsolutePath(), rootPath + File.separator + serviceName);
    }

    public void deleteAppFolder(String serviceName) throws IOException {
        FileUtils.deleteDirectory(new File(rootPath,serviceName));
    }

    private void deleteExistingService(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            if (file.delete()) {
                log.warn("unable to delete parser service {} ", path);
            } else {
                log.info("parser service: {} deleted", path);
            }
        }
    }

    public File unzip(String jarPath, String destStr) {
        try {
            File dest = new File(destStr).getCanonicalFile();
            log.info("unziped class root path: {}", dest.getAbsolutePath());
            JarFile jar = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                File fileInJar = new File(dest, jarEntry.getName());
                if (jarEntry.isDirectory()) {
                    createDir(fileInJar);
                } else {
                    createDir(fileInJar.getParentFile());
                    try (InputStream sourceStream = jar.getInputStream(jarEntry);
                         OutputStream destStream = new FileOutputStream(fileInJar)) {
                        destStream.write(sourceStream.readAllBytes());
                    }
                }
            }
            jar.close();
//                JarEntry jarFileInWar = file.getJarEntry(parserName);
//

//                createWebResourceSet(ResourceSetType.CLASSES_JAR, "/WEB-INF/classes", dest.toURI().toURL(), "/");
            return dest;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void createDir(File fileInJar) {
        boolean exists = fileInJar.exists();
        if (!exists) {
            boolean mkdirs = fileInJar.mkdirs();
            if (!mkdirs)
                throw new RuntimeException("unable to create dir: " + fileInJar.getAbsolutePath());
        }
    }

    public void deleteJar(File jarFile) {
        boolean delete = jarFile.delete();
        if (!delete) {
            log.warn("unable to delete jar file : " + jarFile.getAbsolutePath());
        }
    }
}
