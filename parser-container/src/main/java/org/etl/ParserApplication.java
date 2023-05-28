package org.etl;

import lombok.extern.slf4j.Slf4j;
import org.etl.classloader.ParserResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.jar.JarFile;

@SpringBootApplication
@Slf4j
public class ParserApplication {

    public static final String PARSER_JAR_DIR = "parser_app";

    /**
     * there should be three parts:
     * the container
     * the app
     * the api
     *
     * 1.api is the common bridge for the container and app, which means the container and app should import it but app should set scopt to provided as container will import the real package.
     * 2.the app will implement the real solutions and be packaged in a specific location for container to load when container sees there a new app jar in place
     * 3.the container will start everything and all the supporting components exception the real buession level application.
     *      when the container starts, it will scan a specific location, if there is a jar file in this location and the jar is just what we wanted,
     *      then the conatiner will create a new loader to load it and discard the previous loader, which means the loader and associated classes loaded by this loader will be garbage collected.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
//        SpringApplication.run(ParserApplication.class, args);

        ParserResource parserResource = new ParserResource();
        String userDir = System.getProperty("user.dir");
        String jarDir = userDir + File.separator + "parsers\\target";
        String parserAppDir = userDir +File.separator+ PARSER_JAR_DIR;

        log.info("jar root dir location {}", jarDir);
        File file = new File(jarDir).getCanonicalFile();
        File[] files = file.listFiles();
        assert files != null;
        for (File possibleJar : files) {

            if (possibleJar.getName().endsWith(".jar")){
                JarFile jarFile = new JarFile(possibleJar);
                String jarName = possibleJar.getName();
                jarName = jarName.substring(0, jarName.lastIndexOf("."));
                log.info("unziped class root path: {}",jarDir);
                parserResource.unzip(jarFile, parserAppDir,jarName);
                try {
                    URL url = new File(parserAppDir,jarName).toURI().toURL();
                    URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url},ClassLoader.getSystemClassLoader());

                    Class<? extends Parser> aClass = urlClassLoader.loadClass("org.etl.parser.MarkitParser").asSubclass(Parser.class);
                    Class<? extends Parser> bClass = urlClassLoader.loadClass("org.etl.parser.ICEtParser").asSubclass(Parser.class);

                    Parser o = aClass.getDeclaredConstructor().newInstance();
                    String version = o.version();
                    log.info("version a: {}",version);
                    log.info("version b: {}",bClass.getDeclaredConstructor().newInstance().version());
                    o.parse(new ArrayList<>());
                } catch (MalformedURLException | ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    e.printStackTrace();
                }

                break;
            }
        }


    }

}
