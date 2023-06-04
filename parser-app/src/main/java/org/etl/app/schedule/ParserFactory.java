package org.etl.app.schedule;

import org.etl.service.Parser;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParserFactory {
    private ParserFactory() {
    }
    private static final ParserFactory PARSER_FACTORY = new ParserFactory();

    public static ParserFactory getParserMap() {
        return PARSER_FACTORY;
    }

    private final Map<String, Parser> parserMap = new ConcurrentHashMap<>();

    @Nullable
    public Parser getParserByVersion(String version){
        return parserMap.get(version);
    }

    public Parser putParser(String version,Parser parser){
        return parserMap.put(version,parser);
    }

}
