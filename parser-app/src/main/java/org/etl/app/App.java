package org.etl.app;

import lombok.extern.slf4j.Slf4j;
import org.etl.app.schedule.MyScheduler;
import org.etl.app.schedule.ParserFactory;
import org.etl.service.Application;
import org.etl.service.Context;
import org.etl.service.Parser;
import org.quartz.SchedulerException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;

@SpringBootApplication(scanBasePackages = "org.etl.app")
@Slf4j
public class App implements Application {

    private static MyScheduler myScheduler;
    private ConfigurableApplicationContext run;

    //in order to take advantage of spring-boot-maven-plugin for package, main method is a must-have
    public static void main(String[] args) {

    }
    @Override
    public void start(Context context) {
        ClassLoader loader = App.class.getClassLoader();
        run = new SpringApplicationBuilder(new DefaultResourceLoader(loader), App.class)
                .web(WebApplicationType.NONE)
                .main(App.class)
                .run(context.args());
        Map<String, Parser> beansOfType = run.getBeansOfType(Parser.class);
        for (Map.Entry<String, Parser> stringParserEntry : beansOfType.entrySet()) {
            Parser parser = stringParserEntry.getValue();
            parser.setCache(context.getCacheUtil());
            ParserFactory.getParserMap().putParser(stringParserEntry.getValue().version(), parser);
        }
        String test = run.getEnvironment().getProperty("test");
        System.out.println("property test: " + test);
        String test2 = run.getEnvironment().getProperty("test2");
        System.out.println("property test2: " + test2);
        myScheduler = new MyScheduler();
        myScheduler.schedule();
    }

    @Override
    public void stop(boolean force) {
        try {
            myScheduler.stop();
        } catch (SchedulerException e) {
            log.error("unable to stop scheduler ",e);
        }
        run.stop();
        log.info("app "+this.getClass().getSimpleName()+" is completely shutodwn");
    }

}
