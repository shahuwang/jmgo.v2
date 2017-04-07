package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.NoReachableServerException;
import com.shahuwang.jmgo.exceptions.PoolLimitException;
import com.shahuwang.jmgo.exceptions.ServerClosedException;
import com.shahuwang.jmgo.exceptions.SocketAbendException;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;

import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/4/5.
 */
public class MongoCluster {
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    private Logger logger = Log.getLogger(MongoCluster.class.getName());
    private Servers servers;
    private Servers masters;
    private int syncCount;
    private boolean failFast;
    private SyncChan<Boolean> sync;
    private Condition serverSynced;
    private int reference;
    private ServerAddr[] userSeeds;
    private ServerAddr[] dynaSeeds;
    private boolean direct;
    private IDialer dialer;
    private String setName;

    public MongoCluster(ServerAddr[] userSeeds, boolean direct, boolean failFast, IDialer dialer, String setName){
        this.userSeeds = userSeeds;
        this.reference = 1;
        this.direct = direct;
        this.failFast = failFast;
        this.dialer = dialer;
        this.setName = setName;
        this.sync = new SyncChan<>();
        this.serverSynced = this.rwlock.readLock().newCondition();
        Stats.getInstance().setCluster(1);
        Runnable task = () -> {

        };
        Thread thread = new Thread(task);
        thread.start();
    }

    public void acquire(){
        this.rwlock.readLock().lock();
        this.reference++;
        this.rwlock.readLock().unlock();
    }

    public MongoSocket acquireSocket(Mode mode, boolean slaveOk, Duration syncTimeout
            , Duration socketTimeout, BsonDocument serverTags, int poolLimit) throws NoReachableServerException {
        Duration started = Duration.ZERO;
        int syncCount = 0;
        boolean warnedLimit = false;
        while (true) {
            this.rwlock.readLock().lock();
            while (true) {
                int masterLen = this.masters.len();
                int slaveLen = this.servers.len() - masterLen;
                logger.debug("Cluster has {} known masters and {} known slaves", masterLen, slaveLen);
                if (masterLen > 0 && !(slaveOk && mode == Mode.SECONDARY) || slaveLen > 0 && slaveOk) {
                    break;
                }
                if (masterLen > 0 && mode == Mode.SECONDARY && this.masters.hasMongos()) {
                    break;
                }
                if (started.isZero()) {
                    started = Duration.ofMillis(System.currentTimeMillis());
                    syncCount = this.syncCount;
                } else if (!syncTimeout.isZero() && started.getSeconds() * 1000 < (System.currentTimeMillis() - syncCount) || this.failFast && this.syncCount != syncCount) {
                    this.rwlock.readLock().unlock();
                    throw new NoReachableServerException();
                }
                logger.info("Waiting for servers to synchronize...");
                this.syncServers();
                try {

                    this.serverSynced.wait();
                } catch (InterruptedException e) {
                    logger.catching(e);
                }
            }

            MongoServer server = null;
            if (slaveOk) {
                server = this.servers.bestFit(mode, serverTags);
            } else {
                server = this.masters.bestFit(mode, serverTags);
            }
            this.rwlock.readLock().unlock();
            if (server == null) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.catching(e);
                }
                continue;
            }
            try {
                server.acquireSocket(poolLimit, socketTimeout);
            }catch (PoolLimitException e){
                if(!warnedLimit){
                    warnedLimit = true;
                    logger.info("warning: per-server connection limit reached.");
                }
                try{
                    Thread.sleep(100);
                }catch (InterruptedException err){
                    logger.catching(err);
                }
                continue;
            }catch (ServerClosedException e){
                this.removeServer(server);
                this.syncServers();
                continue;
            }catch (SocketAbendException e){
                if(!slaveOk){
                    //TODO
                }
            }
            //TODO
            //server.acquireSocket();
        }
    }

    private MasterAck isMaster(MongoSocket socket){
        //TODo
        MongoSession session = new MongoSession(Mode.MONOTONIC, this, Duration.ofSeconds(10));
        session.setSocket(socket);
        //session.Run("ismaster");
        return null;
    }

    private void removeServer(MongoServer server){
        this.rwlock.writeLock().lock();
        this.masters.remove(server);
        MongoServer other = this.servers.remove(server);
        this.rwlock.writeLock().unlock();
        if(other != null){
            other.close();
            logger.info("Removed server {} from cluster", server.getAddr());
        }
        server.close();
    }

    private void syncServers() {
        this.sync.offer(true);
    }
}
