package org.etl;


import org.etl.entity.Trade;

import java.util.List;

public interface Parser {

    public void parse(List<Trade> list);

    public String version();
}
