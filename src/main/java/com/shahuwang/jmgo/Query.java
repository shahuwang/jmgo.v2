package com.shahuwang.jmgo;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rickey on 2017/4/5.
 */
public class Query {
    private Lock lock = new ReentrantLock();
    private MongoSession session;
    private QueryConfig queryConfig;
    public Query(MongoSession session, QueryConfig queryConfig){
        this.session = session;
        this.queryConfig = queryConfig;
    }

    public void One(){
        this.lock.lock();
        MongoSession session = this.session;
        OpQuery op = this.queryConfig.getOp().clone();
        this.lock.unlock();

    }


    public QueryConfig getQueryConfig() {
        return queryConfig;
    }
}
