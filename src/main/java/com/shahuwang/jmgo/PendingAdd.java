package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/12.
 */
class PendingAdd {
    protected MongoServer server;
    protected ServerInfo info;

    public PendingAdd(MongoServer server, ServerInfo info) {
        this.server = server;
        this.info = info;
    }
}
