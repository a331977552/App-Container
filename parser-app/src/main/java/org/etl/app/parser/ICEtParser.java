package org.etl.app.parser;

import org.etl.service.Parser;
import org.etl.service.entity.Trade;
import org.etl.service.service.CacheUtil;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ICEtParser implements Parser {
    private CacheUtil cache;

    @Override
    public void parse(List<Trade> list) {
        System.out.println("ice parsing start"+ list.size());
        long start = System.currentTimeMillis();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("ice parsing end, time consumed: "+(System.currentTimeMillis()-start)+" withc cache: "+ cache.getA(new Trade()) );
    }

    @Override
    public void setCache(CacheUtil cache) {
        this.cache = cache;
    }

    @Override
    public String version() {
        return "ice";
    }
}
