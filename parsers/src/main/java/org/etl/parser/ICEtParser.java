package org.etl.parser;

import org.etl.Parser;
import org.etl.entity.Trade;

import java.util.List;

public class ICEtParser implements Parser {
    @Override
    public void parse(List<Trade> list) {
        System.out.println("test ------------ice"+ list.size());
    }

    @Override
    public String version() {
        return "ice";
    }
}
