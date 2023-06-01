package org.etl.core;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Bootstrap {

    private Class<?> appMain;
    public void start(String rootPath, String serviceName){
        if (appMain!=null){
            Method stop = null;
            try {
                stop = appMain.getMethod("stop");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            try {
                Object invoke = stop.invoke(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
        try {
            File file = new File(rootPath, serviceName + File.separator + "BOOT-INF" + File.separator + "classes"+File.separator);
            File file1 = new File(rootPath, serviceName + File.separator + "BOOT-INF" + File.separator + "lib"+File.separator);
            List<URL> urls = new ArrayList<>();
            try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(file1.getAbsolutePath()), "*.jar")) {
                for (Path path : directoryStream) {
                    urls.add(path.toUri().toURL());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            urls.add(file.toURI().toURL());

            log.info("classpath: {},{}",file.getAbsolutePath(),file1.getAbsolutePath());
            ConatinerClassLoader parserServiceClassLoader = new ConatinerClassLoader(urls.toArray(new URL[0]));
            appMain = parserServiceClassLoader.loadClass("org.etl.parser.App");
            Method main = appMain.getMethod("main", String[].class);
            String [] args= new String[]{};
            main.invoke(null,(Object) args);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
//            URL resource = parserServiceClassLoader.getResource("application.properties");

        //            FileInputStream fileInputStream = new FileInputStream(new File(resource.getFile()));
//            byte[] bytes = fileInputStream.readAllBytes();
//            String s = new String(bytes, StandardCharsets.UTF_8);
//            System.out.println(s);
        //todo
//            read xml config, and load classes
//            Class<? extends Parser> aClass = parserServiceClassLoader.loadClass("org.etl.parser.MarkitParser").asSubclass(Parser.class);
//            Class<? extends Parser> bClass = parserServiceClassLoader.loadClass("org.etl.parser.ICEtParser").asSubclass(Parser.class);
//            ParserFactory.getParserMap().putParser("markit", aClass.getDeclaredConstructor().newInstance());
//            ParserFactory.getParserMap().putParser("ice", bClass.getDeclaredConstructor().newInstance());

    }
}
