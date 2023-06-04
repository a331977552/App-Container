package org.etl.core;

public class Repository {
    private final String location;
    private final RepositoryType type;

    public Repository(String location, RepositoryType type) {
        this.location = location;
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public RepositoryType getType() {
        return type;
    }

    public enum RepositoryType {
        /**
         * *.jar
         */
        GLOB,
        /**
         * directory
         */
        DIR,
        /**
         * .jar
         */
        JAR,
        /**
         * URL
         */
        URL
    }
}