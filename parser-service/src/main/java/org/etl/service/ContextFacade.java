package org.etl.service;

import org.etl.service.service.CacheUtil;

public class ContextFacade implements Context {
    private final Context context;

    public ContextFacade(Context context) {
        this.context = context;
    }

    @Override
    public CacheUtil getCacheUtil() {
        return context.getCacheUtil();
    }

    @Override
    public String[] args() {
        return context.args();
    }
}
