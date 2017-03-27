package com.shahuwang.jmgo;


import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/3/22.
 */
public class MongoServer {
    private ServerAddr addr;
    private List<MongoSocket> unusedSockets;
    private List<MongoSocket> liveSockets;
    private boolean closed;
    private boolean abended;
    private IDialer dialer;
    private SyncChan<Boolean> sync;
    private Duration pingValue;
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();

    Logger logger = Log.getLogger(MongoServer.class.getName());

    public MongoServer(ServerAddr addr, SyncChan<Boolean> sync, IDialer dialer){
        this.addr = addr;
        this.sync = sync;
        this.dialer = dialer;
        this.pingValue = Duration.ofHours(1);
        Thread t = new Thread(() -> {
            pinger(true);
        });
        t.start();
    }

    public void abendSocket(MongoSocket socket){

    }

    public boolean isAbended() {
        return abended;
    }

    public ServerAddr getAddr() {
        return addr;
    }

    public ReadWriteLock getRwlock() {
        return rwlock;
    }

    public Duration getPingValue() {
        return pingValue;
    }

    public List<MongoSocket> getUnusedSockets() {
        return unusedSockets;
    }

    public List<MongoSocket> getLiveSockets() {
        return liveSockets;
    }

    protected void pinger(boolean loop){

    }
}
