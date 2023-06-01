package org.etl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.Bootstrap;
import org.etl.core.FileMonitorService;
import org.etl.core.JarDeployer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ContainerApp {

    public static final String PARSER_APP_DIR = "parser_app";

    /**
     * there should be three parts:
     * the container
     * the app
     * the api
     * <p>
     * 1.api is the common bridge for the container and app, which means the container and app should import it but app should set scopt to provided as container will import the real package.
     * 2.the app will implement the real solutions and be packaged in a specific location for container to load when container sees there a new app jar in place
     * 3.the container will start everything and all the supporting components exception the real buession level application.
     * when the container starts, it will scan a specific location, if there is a jar file in this location and the jar is just what we wanted,
     * then the conatiner will create a new loader to load it and discard the previous loader, which means the loader and associated classes loaded by this loader will be garbage collected.
     *      todo
     *      1.(done) test while running parser, replace a jar and load it
     *      2.(done) delete jar file after unzip
     *      3.package spring projects and put it under this folder to execute.
     */
    public static void main(String[] args) throws IOException {


        String userDir = System.getProperty("user.dir");
        String parserAppDir = userDir + File.separator + PARSER_APP_DIR;
        JarDeployer jarDeployer = new JarDeployer(parserAppDir);

        File file = new File(parserAppDir).getCanonicalFile();
        if (!file.exists()){
            boolean mkdirs = file.mkdirs();
            if (!mkdirs){
                throw new RuntimeException("unable to create parser service root path: " +file.getAbsolutePath());
            }
        }
        if(file.isDirectory()){
            File[] files = file.listFiles();
            Objects.requireNonNull(files);
            Optional<File> first = Arrays.stream(files).filter(f -> f.getName().endsWith(".jar")).findFirst();
            String serviceName;
            if (first.isPresent()){
                File jar = first.get();
                serviceName = getServiceName(jar);
                jarDeployer.deploy(serviceName, jar);
            }


            Optional<File> existProject = Arrays.stream(files).filter(File::isDirectory).findFirst();
            if (existProject.isPresent()){
                File project = existProject.get();
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.start(project.getParentFile().getAbsolutePath(),project.getName());
            }

        }else{
            throw new RuntimeException("unable to find parser service root path: " +file.getAbsolutePath());
        }


        FileMonitorService fileMonitorService = new FileMonitorService(file);
        fileMonitorService.setFileAlterationListenerAdaptor(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File jar) {
                    log.info("new jar arrived. deploying!!!!");
                String rootPath = jar.getParentFile().getAbsolutePath();
                String serviceName = getServiceName(jar);
                jarDeployer.deploy(serviceName,jar);
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.start(rootPath,serviceName);
            }
        });
        try {
            fileMonitorService.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getServiceName(File jar) {
        String serviceName;
        serviceName = jar.getName();
        serviceName = serviceName.substring(0, serviceName.lastIndexOf("."));
        return serviceName;
    }

}
