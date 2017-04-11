package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.*;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarException;

/**
 * Created by rickey on 2017/4/5.
 */
public class MongoSession {
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private QueryConfig queryConfig = new QueryConfig();
    private MongoSocket slaveSocket;
    private MongoSocket masterSocket;
    private Mode consistency;
    private MongoCluster cluster_;
    private Duration syncTimeout;
    private Duration sockTimeout;
    private int poolLimit;
    private boolean slaveOk;
    private OpQuery safeOp;
    private String defaultdb;
    private Logger logger = Log.getLogger(MongoSession.class.getName());
    public MongoSession(Mode consistency, MongoCluster cluster, Duration timeout){
        cluster.acquire();
        this.cluster_ = cluster;
        this.sockTimeout = timeout;
        this.syncTimeout = timeout;
        this.poolLimit = 4096;
        this.setMode(consistency, true);
        this.setSafe(new Safe());
        this.queryConfig.setPrefetch(0.25f);
    }

    public BsonDocument run(BsonDocument cmd)throws SessionClosedException, NoReachableServerException, JmgoException, NotFoundError{
        return this.DB("admin").run(cmd);
    }

    public BsonDocument run(String cmd)throws JmgoException, NotFoundError{
        return this.DB("admin").run(cmd);
    }

    public Database DB(String name){
        if(name == ""){
            name = this.defaultdb;
        }
        return new Database(this, name);
    }
    public void setMode(Mode consistency, boolean refresh){
        this.lock.writeLock().lock();
        this.consistency = consistency;
        if(refresh){
            this.slaveOk = this.consistency != Mode.STRONG;
            this.unsetSocket();
        }else if(this.consistency == Mode.STRONG){
            this.slaveOk = false;
        }else if(this.masterSocket == null){
            this.slaveOk = true;
        }
        this.lock.writeLock().unlock();
    }

    public void setSafe(Safe safe){
        this.lock.writeLock().lock();
        this.safeOp = null;
        this.ensureSafe(safe);
        this.lock.writeLock().unlock();
    }

    protected void prepareQuery(OpQuery op){
        this.lock.readLock().lock();
        op.setMode(this.consistency);
        if(this.slaveOk){
            int flag = (op.getFlags() | OpQueryFlag.flagSlaveOk);
            op.setFlags(flag);
        }
        this.lock.readLock().unlock();
    }

    private void ensureSafe(Safe safe){
        if(safe == null){
            return;
        }
        Object w = null;
        if(safe.getwMode() != ""){
            w = safe.getwMode();
        }else if(safe.getW() > 0){
            w = safe.getW();
        }
        //TODO
    }

    private void unsetSocket(){
        if(this.masterSocket != null){
            this.masterSocket.release();
        }
        if(this.slaveSocket != null){
            this.slaveSocket.release();
        }
        this.masterSocket = null;
        this.slaveSocket = null;
    }

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
                    this.syncTimeout, this.sockTimeout, tags, this.poolLimit);
//        }catch (SessionClosedException e){
//            this.lock.writeLock().unlock();
//            throw new SessionClosedException();
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
//            try {
                this.setSocket(socket);
//            }catch (SlaveSocketReservedException e){
//                logger.catching(e);
//            }catch (MasterSocketReservedException e){
//                logger.catching(e);
//            }
        }
        if (!slaveOk && this.consistency == Mode.MONOTONIC){
            this.slaveOk = false;
        }
        this.lock.writeLock().unlock();
        return socket;
    }

    private void socketLogin(MongoSocket socket){
        //TODO
    }

    protected void setSocket(MongoSocket socket){
        ServerInfo info = socket.acquire();
        if(info.isMaster()){
            if(this.masterSocket != null){
                throw new RuntimeException("setSocket(master) with existing master socket reserved");
            }
            this.masterSocket = socket;
        }else{
            if(this.slaveSocket != null){
                throw new RuntimeException("setSocket(slave) with existing slave socket reserved");
            }
            this.slaveSocket= socket;
        }
    }

    protected MongoCluster cluster() {
        if(this.cluster_ == null){
            throw new RuntimeException("Session already closed");
        }
        return this.cluster_;
    }

    public void close(){
        this.lock.writeLock().lock();
        if(this.cluster_ != null){
            this.unsetSocket();
            try {
                this.cluster_.release();
            }catch (ReferenceZeroException e){
                logger.catching(e);
            }
            this.cluster_ = null;
        }
        this.lock.writeLock().unlock();
    }


    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public QueryConfig getQueryConfig() {
        return queryConfig;
    }
}
