package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.etl.core.BootStrap;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
@Component
public class JarDeployer {
    private final String rootPath = BootStrap.APP_HOME;

    private static final JarDeployer JAR_DEPLOYER =new JarDeployer();

    public static JarDeployer getInstance(){
        return JAR_DEPLOYER;
    }


    private JarDeployer() {
    }





    public void deleteExistingExplodedApp(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            if (file.delete()) {
                log.info("app  {} deleted", path);
            } else {
                log.warn("unable to delete app {} ", path);
            }
        }
    }
    public File unzip(File jarPath, String destStr) {
        return this.unzip(jarPath.getAbsolutePath(),destStr);
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
