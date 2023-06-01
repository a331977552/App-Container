package org.etl.parser;

import lombok.extern.slf4j.Slf4j;
import org.etl.schedule.MyScheduler;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;

@SpringBootApplication(scanBasePackages = "org.etl.parser")
@Slf4j
public class App {


    public static void main(String[] args) {
        ClassLoader loader = App.class.getClassLoader();
        if (loader instanceof URLClassLoader classLoader){
            URL[] urLs = classLoader.getURLs();
            log.info("");
            log.info("classpath in client*****:");
            for (URL urL : urLs) {
                System.out.println(urL.toString());
            }
            log.info("classpath in client end ");
        }



        ConfigurableApplicationContext run = new SpringApplicationBuilder(new DefaultResourceLoader(loader), App.class)
                .web(WebApplicationType.NONE)
                .main(App.class)
//                .initializers(applicationContext -> applicationContext.setClassLoader(classLoader))
                .run(args);

        String test = run.getEnvironment().getProperty("test");
        System.out.println("property test: "+test);

        new MyScheduler().schedule();

    }
}
