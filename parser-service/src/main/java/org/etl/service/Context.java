package org.etl.service;

import org.etl.service.service.CacheUtil;

public interface Context {

    public CacheUtil getCacheUtil();

    public String[] args();
}
