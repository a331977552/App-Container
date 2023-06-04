package org.etl.app;

import lombok.extern.slf4j.Slf4j;
import org.etl.app.schedule.MyScheduler;
import org.etl.app.schedule.ParserFactory;
import org.etl.service.Parser;
import org.etl.service.ReferenceService;
import org.etl.service.ReferenceServiceReceiver;
import org.quartz.SchedulerException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "org.etl.app")
@Slf4j
public class App implements ReferenceServiceReceiver {


    private static MyScheduler myScheduler;
    private static ReferenceService referenceService;

    public static void main(String[] args) {
        ClassLoader loader = App.class.getClassLoader();
//        Thread.currentThread().setContextClassLoader(loader);
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
        Map<String, Parser> beansOfType = run.getBeansOfType(Parser.class);
        for (Map.Entry<String, Parser> stringParserEntry : beansOfType.entrySet()) {
            Parser parser = stringParserEntry.getValue();
            parser.setCache(referenceService.getCacheUtil());
            ParserFactory.getParserMap().putParser(stringParserEntry.getValue().version(),parser);
        }
        String test = run.getEnvironment().getProperty("test");
        System.out.println("property test: "+test);
        myScheduler = new MyScheduler();
        myScheduler.schedule();
    }


    public static void stop(boolean force){
        try {
            myScheduler.stop();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReferenceServiceArrived(ReferenceService referenceService) {
        App.referenceService = referenceService;
    }
}
