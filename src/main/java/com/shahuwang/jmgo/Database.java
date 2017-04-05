package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/4/5.
 */
public class Database {
    private MongoSession session;
    private String name;

    public MongoSession getSession() {
        return session;
    }

    public String getName() {
        return name;
    }
}
