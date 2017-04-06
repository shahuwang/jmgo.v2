package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.NoReachableServerException;
import com.shahuwang.jmgo.exceptions.SessionClosedException;
import org.bson.BsonDocument;
import org.bson.BsonInt32;

/**
 * Created by rickey on 2017/4/5.
 */
public class Database {
    private MongoSession session;
    private String name;

    public Database(MongoSession session, String name){
        this.session = session;
        this.name = name;
    }

    public MongoSession getSession() {
        return session;
    }

    public String getName() {
        return name;
    }

    public BsonDocument run(BsonDocument cmd)throws SessionClosedException, NoReachableServerException{
        MongoSocket socket = this.session.acquireSocket(true);
        BsonDocument ret = this.run(socket, cmd);
        socket.release();
        return ret;
    }

    public BsonDocument run(String cmd)throws SessionClosedException, NoReachableServerException{
        MongoSocket socket = this.session.acquireSocket(true);
        BsonDocument ret = this.run(socket, cmd);
        socket.release();
        return ret;
    }

    private BsonDocument run(MongoSocket socket, BsonDocument cmd){
       MongoSession session = this.session;
       session.getLock().readLock().lock();
       OpQuery op = session.getQueryConfig().getOp().clone();
       session.getLock().readLock().unlock();
       op.setQuery(cmd);
       op.setCollection(this.name + ".$cmd");
       session.prepareQuery(op);
    }

    private BsonDocument run(MongoSocket socket, String cmd){
        BsonDocument query = new BsonDocument(cmd, new BsonInt32(1));
        return run(socket, query);
    }
}
