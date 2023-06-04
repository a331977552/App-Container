package org.etl.service.service;

import org.etl.service.entity.Trade;

public class CacheUtil {

    public String getA(Trade key){
        return key.getFeed()+" got";
    }
}
