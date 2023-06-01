package org.etl.parser;

import org.etl.Parser;
import org.etl.entity.Trade;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkitParser implements Parser {
    @Override
    public void parse(List<Trade> list) {
        System.out.println("Markit parsing start"+ list.size());
        long start = System.currentTimeMillis();

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Markit parsing end, time consumed: "+(System.currentTimeMillis()-start));
    }
    public String version() {
        return "markit";
    }

}
