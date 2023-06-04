package org.etl.app.schedule;

import lombok.extern.slf4j.Slf4j;
import org.etl.service.Parser;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
public class ParsingJob implements Job {
    List<String> list = Arrays.asList("markit","ice");

    @Override
    public void execute(JobExecutionContext context) {
        int i = new Random().nextInt(2);

        String s = list.get(i);
        Parser parserByVersion = ParserFactory.getParserMap().getParserByVersion(s);
        if (parserByVersion!=null){
            log.info("start to run parsing service with version :{}",s);
            parserByVersion.parse(new ArrayList<>());
        }else{
            log.warn("parser with version {} doesn't exist", s);
        }
    }
}
