package com.librato;

import java.util.ArrayList;
import java.util.List;

public class ExampleConfig {

    public int id;
    public String name;
    public List<Thing> things = new ArrayList();

    public static class Thing {
        public String name;

        public Thing() {
        }

        public Thing(String name) {
            this.name = name;
        }
    }
}
