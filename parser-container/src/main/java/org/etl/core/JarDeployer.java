package org.etl.core;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class JarDeployer {
    private final String rootPath;
    ParserResource parserResource;


    public JarDeployer(String rootPath) {
        this.rootPath = rootPath;
        parserResource = new ParserResource();
    }

    public File deploy(String serviceName, File jar) {
        log.info("deploy parser jar :{} with name: {}", jar.getAbsolutePath(), serviceName);
        parserResource.deleteUnzippedContent(rootPath + File.separator + serviceName);
        File unzip = parserResource.unzip(jar.getAbsolutePath(), rootPath + File.separator + serviceName);
        parserResource.deleteJar(jar);
        return unzip;
    }
}
