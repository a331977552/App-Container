package org.etl.service;


import org.etl.service.entity.Trade;
import org.etl.service.service.CacheUtil;

import java.util.List;

public interface Parser {

    public void parse(List<Trade> list);

    public void setCache(CacheUtil cache);

    public String version();
}
