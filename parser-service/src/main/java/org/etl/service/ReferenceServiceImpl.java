package org.etl.service;

import org.etl.service.service.CacheUtil;

public class ReferenceServiceImpl implements ReferenceService {

    private final CacheUtil cacheUtil;
    {
        cacheUtil = new CacheUtil();
    }

    @Override
    public CacheUtil getCacheUtil() {
        return cacheUtil;
    }
}
