package org.etl.parser;

import org.etl.Parser;
import org.etl.entity.Trade;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ICEtParser implements Parser {
    @Override
    public void parse(List<Trade> list) {
        System.out.println("ice parsing start"+ list.size());
        long start = System.currentTimeMillis();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("ice parsing end, time consumed: "+(System.currentTimeMillis()-start));
    }

    @Override
    public String version() {
        return "ice";
    }
}
