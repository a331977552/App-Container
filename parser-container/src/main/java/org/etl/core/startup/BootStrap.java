package org.etl.core.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.etl.core.AppWrapper;
import org.etl.core.FileMonitorService;
import org.etl.core.Repository;
import org.etl.core.Server;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static org.etl.core.startup.ClassLoaderFactory.validateFile;


/**
 * TODO currently, app name is determined by jar name, we need to change this by using manifest name
 */
@Slf4j
@SpringBootApplication
public class BootStrap {

    public static final String APP_DIR_NAME = "parser_app";

    private static final String userHome = System.getProperty("user.dir");
    public static final String APP_HOME = userHome + File.separator + APP_DIR_NAME;

    private static final Object lock = new Object();
    private Server server;

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
    public static void main(String[] args) {

//        MySecurityManager.forbidSystemExitCall();
        BootStrap bootStrap = new BootStrap();
        try {
            bootStrap.init();
            bootStrap.start();
        } catch (Exception e) {
            log.error("unexpected error encountered, system exiting", e);
        }
    }

    private void start() throws Exception {
        server.start();
        log.info("server started");
    }

    private void init() throws IOException {
        File rootPath = new File(APP_HOME).getCanonicalFile();
        if (validateFile(rootPath, Repository.RepositoryType.DIR)) {
            throw new RuntimeException("unable to create parser service root path: " + rootPath.getAbsolutePath());
        }
        ConfigurableApplicationContext run = new SpringApplicationBuilder(new DefaultResourceLoader(this.getClass().getClassLoader()), BootStrap.class)
                .web(WebApplicationType.NONE)
//                .initializers(applicationContext -> applicationContext.setClassLoader(classLoader))
                .run();
        server = run.getBean(Server.class);

    }

}
