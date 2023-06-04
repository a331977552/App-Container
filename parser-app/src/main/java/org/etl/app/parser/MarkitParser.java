package org.etl.app.parser;

import org.etl.service.Parser;
import org.etl.service.entity.Trade;
import org.etl.service.service.CacheUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkitParser implements Parser {
    private CacheUtil cache;

    @Override
    public void parse(List<Trade> list) {
        System.out.println("Markit parsing start"+ list.size());
        long start = System.currentTimeMillis();
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Trade trade = new Trade();
        trade.setFeed("!23");
        System.out.println(cache.getA(trade));
        System.out.println("Markit parsing end, time consumed: "+(System.currentTimeMillis()-start));
    }

    @Override
    public void setCache(CacheUtil cache) {
        System.out.println("cache set"+ cache);
        this.cache = cache;
    }

    public String version() {
        return "markit";
    }

}
