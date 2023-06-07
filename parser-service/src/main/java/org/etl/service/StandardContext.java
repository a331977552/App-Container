package org.etl.service;

import org.etl.service.service.CacheUtil;

public class StandardContext implements Context {

    private String[] args;
    private CacheUtil cacheUtil;

    public void setCacheUtil(CacheUtil cacheUtil) {
        this.cacheUtil = cacheUtil;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    @Override
    public CacheUtil getCacheUtil() {
        return cacheUtil;
    }

    @Override
    public String[] args() {
        return args;
    }
}
