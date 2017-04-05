package com.shahuwang.jmgo;

import org.bson.BsonDocument;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/4/5.
 */
public class Collection {
    private Database database;
    private String name;
    private String fullname;

    public Query find(BsonDocument query){
        MongoSession session = this.database.getSession();
        ReentrantReadWriteLock lock = session.getLock();
        lock.readLock().lock();
        Query q = new Query(session, session.getQueryConfig());
        lock.readLock().unlock();
        q.getQueryConfig().getOp().setQuery(query);
        q.getQueryConfig().getOp().setCollection(this.fullname);
        return q;
    }
}
