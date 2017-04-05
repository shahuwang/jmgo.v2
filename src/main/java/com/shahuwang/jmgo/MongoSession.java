package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.NoReachableServerException;
import com.shahuwang.jmgo.exceptions.SessionClosedException;
import org.bson.BsonDocument;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/4/5.
 */
public class MongoSession {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private QueryConfig queryConfig;
    private MongoSocket slaveSocket;
    private MongoSocket masterSocket;
    private boolean slaveOk;
    private Mode consistency;
    private MongoCluster cluster_;

    protected MongoSocket acquireSocket(boolean slaveOk)throws SessionClosedException, NoReachableServerException {
        this.lock.readLock().lock();
        if(this.slaveSocket != null
                && this.slaveOk && slaveOk &&
                (this.masterSocket == null ||
                        this.consistency != Mode.PRIMARY_PREFERRED &&
                                this.consistency != Mode.MONOTONIC)){
            MongoSocket socket = this.slaveSocket;
            this.lock.readLock().unlock();
            return socket;
        }
        if(this.masterSocket != null){
            MongoSocket socket = this.masterSocket;
            socket.acquire();
            this.lock.readLock().unlock();
            return socket;
        }
        this.lock.readLock().unlock();

        // 还是没有拿到合适的，用更强的锁再试多一次，但是不理解这个原因
        this.lock.writeLock().lock();

        if(this.slaveSocket != null
                && this.slaveOk && slaveOk &&
                (this.masterSocket == null ||
                        this.consistency != Mode.PRIMARY_PREFERRED &&
                                this.consistency != Mode.MONOTONIC)){
            MongoSocket socket = this.slaveSocket;
            this.lock.writeLock().unlock();
            return socket;
        }
        if(this.masterSocket != null){
            MongoSocket socket = this.masterSocket;
            socket.acquire();
            this.lock.writeLock().unlock();
            return socket;
        }

        // 还是没拿到合适的，创建一个新的
        BsonDocument tags = this.queryConfig.getOp().getServerTags();
        MongoSocket socket = null;
        try {
            socket = this.cluster().acquireSocket(
                    this.consistency, this.slaveOk && this.slaveOk,
                    this.syncTimeout, this.sockTimeout, tags.toArray(new BsonElement[tags.size()]), this.poolLimit);
        }catch (SessionClosedException e){
            this.lock.writeLock().unlock();
            throw new SessionClosedException();
        }catch (NoReachableServerException e){
            this.lock.writeLock().unlock();
            throw new NoReachableServerException();
        }
        try{
            this.socketLogin(socket);
        }catch (Exception e){
            socket.release();
            this.lock.writeLock().unlock();
            throw e;
        }

        if(this.consistency != Mode.EVENTULA || this.slaveSocket != null){
            try {
                this.setSocket(socket);
            }catch (SlaveSocketReservedException  e){
                logger.catching(e);
            }catch (MasterSocketReservedException e){
                logger.catching(e);
            }
        }
        if (!slaveOk && this.consistency == Mode.MONOTONIC){
            this.slaveOk = false;
        }
        this.lock.writeLock().unlock();
        return socket;
    }

    protected MongoCluster cluster() {
        if(this.cluster_ == null){
            throw new RuntimeException("Session already closed");
        }
        return this.cluster_;
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public QueryConfig getQueryConfig() {
        return queryConfig;
    }
}
