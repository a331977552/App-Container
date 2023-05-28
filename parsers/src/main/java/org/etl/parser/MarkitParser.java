package org.etl.parser;

import org.etl.Parser;
import org.etl.entity.Trade;

import java.util.List;

public class MarkitParser implements Parser {
    @Override
    public void parse(List<Trade> list) {
        System.out.println("test ------------test"+ list.size());
    }

    @Override
    public String version() {
        return "markit";
    }
}
