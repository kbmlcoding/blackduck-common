package com.synopsys.integration.blackduck.service;

import java.util.List;

import com.synopsys.integration.blackduck.api.core.BlackDuckResponse;

public class FruitTest extends BlackDuckResponse {
    public String name;
    public FruitCollection fruits;

    public enum PossibleFruits {
        APPLE,
        BANANA
    }

    public class Fruits {
        public boolean apple;
        public boolean banana;
    }

    public class FruitCollection {
        public List<PossibleFruits> possibleFruits;
        public List<Fruits> nestedList;
    }
}
