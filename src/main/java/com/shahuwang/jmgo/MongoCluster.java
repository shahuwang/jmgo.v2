package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.*;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/4/5.
 */
public class MongoCluster {
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    private Logger logger = Log.getLogger(MongoCluster.class.getName());
    private Servers servers = new Servers();
    private Servers masters = new Servers();
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
    private Duration syncShortDelay = Duration.ofMillis(500);
    private Duration syncServersDelay = Duration.ofSeconds(30);

    public MongoCluster(ServerAddr[] userSeeds, boolean direct, boolean failFast, IDialer dialer, String setName){
        this.userSeeds = userSeeds;
        this.reference = 1;
        this.direct = direct;
        this.failFast = failFast;
        this.dialer = dialer;
        this.setName = setName;
        this.sync = new SyncChan<>();
        this.serverSynced = this.rwlock.writeLock().newCondition();
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
            this.rwlock.writeLock().lock();
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

                    this.serverSynced.await();
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
            this.rwlock.writeLock().unlock();
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

    protected void removeServer(MongoServer server){
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

    private void syncServersLoop(){
        while (true){
            this.rwlock.readLock().lock();
            if(this.reference == 0){
                this.rwlock.readLock().unlock();
                break;
            }
            this.reference++;
            boolean direct = this.direct;
            this.rwlock.readLock().unlock();
            this.syncServersIteration(direct);
            this.sync.poll();
            try {

                this.release();
            }catch (ReferenceZeroException e){
                logger.catching(e);
            }
            if(!this.failFast){
                try {
                    Thread.sleep(syncShortDelay.toMillis());
                }catch (InterruptedException e){
                    logger.catching(e);
                }
            }
            this.rwlock.readLock().lock();
            if(this.reference == 0){
                this.rwlock.readLock().unlock();
                break;
            }
            this.syncCount++;
            this.serverSynced.signalAll();
            boolean restart = !direct && this.masters.empty() || this.servers.empty();
            this.rwlock.readLock().unlock();
            if(restart){
                try {
                    Thread.sleep(syncShortDelay.toMillis());
                }catch (InterruptedException e){
                    logger.catching(e);
                }
                continue;
            }
            this.sync.pollTimeout(syncShortDelay.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void syncServersIteration(boolean direct){
        Lock m = new ReentrantLock();
    }

    public void release() throws ReferenceZeroException{
        this.rwlock.writeLock().lock();
        if(this.reference == 0){
            this.rwlock.writeLock().unlock();
            throw new ReferenceZeroException("cluster release with references == 0");
        }
        this.reference--;
        logger.info("Cluster {} released (refs={})", this, this.reference);
        if(this.reference == 0) {
            for(MongoServer server: this.servers.getSlice()){
                server.close();
            }
            this.syncServers();
            Stats.getInstance().setCluster(-1);
        }
        this.rwlock.writeLock().unlock();
    }

    protected TopologyInfo syncServer(MongoServer server) throws SyncServerException{
        Duration syncTimeout = null;
        if (BuildConfig.getInstance().getRacedetector()){
            synchronized (GlobalMutex.class){
                syncTimeout = this.syncSocketTimeout;
            }
        } else {
            syncTimeout = Cluster.syncSocketTimeout;
        }

        ServerAddr addr = server.getAddr();
        logger.info("SYNC Processing {} ...", addr.getTcpaddr());
        MasterAck result = null;
        SyncServerException err = null;
        int retry = -1;
        while (true) {
            retry = retry + 1;
            if(retry == 3 || retry == 1 && this.failFast){
                throw err;
            }
            if (retry > 0) {
                // TODO
                // 这里可能会出现possibleTimeout的错误，但是找遍mgo的代码也没找到这个error的定义是哪里
            }
            MongoSocket socket = null;
            try {
                socket = server.acquireSocket(0, syncTimeout);
            }catch (JmgoException e){
                String errmsg = String.format("SYNC failed to get socket to %s: %s", addr.getTcpaddr().toString(), e.getMessage());
                err = new SyncServerException(errmsg);
                continue;
            }
            try {
                this.isMaster(socket, result);
                socket.release();
            }catch (JmgoException e){
                socket.release();
                String errmsg = String.format("SYNC Command 'ismaster' to %s failed: %s", addr.getTcpaddr().toString(), e.getMessage());
                err = new SyncServerException(errmsg);
                continue;
            }
            logger.info("SYNC Result of 'ismaster' from %s: %s", addr.getTcpaddr().toString(), result.toString());
            break;
        }
        if(this.setName != "" && result.getSetName() != this.setName){
            String msg = String.format("SYNC Server %s is not a member of replica set %s", addr.getTcpaddr().toString(), this.setName);
            logger.info(msg);
            throw new SyncServerException(msg);
        }
        if (result.isMaster()){
            logger.info("SYNC {} is a master", addr.getTcpaddr().toString());
            if(!server.getInfo().isMaster()){
                Stats.getInstance().setConn(-1, false);
                Stats.getInstance().setConn(1, true);
            }
        }else if(result.isSecondary()){
            logger.info("SYNC {} is a slave", addr.getTcpaddr().toString());
        }else{
            logger.info("SYNC {} is neither a master nor a slave.", addr.getTcpaddr().toString());
            throw new SyncServerException(String.format("SYNC %s is not a master nor slave", addr.getTcpaddr().toString()));
        }
        ServerInfo info = new ServerInfo(result.isMaster(), result.getMsg() == "isdbgrid", result.getTags(), result.getSetName(), result.getMaxWireVersion());
        List<ServerAddr> hosts = new ArrayList<>();
        if (result.getPrimary() != null) {
            hosts.add(result.getPrimary());
        }
        for(ServerAddr h: result.getHosts()){
            hosts.add(h);
        }
        for(ServerAddr p: result.getPassives()){
            hosts.add(p);
        }

        logger.info("SYNC {} knows about the following peers: {}", addr.getTcpaddr().toString(), hosts);
        return new TopologyInfo(info, hosts.toArray(new ServerAddr[hosts.size()]));
    }


    protected MongoServer server(ServerAddr addr){
        this.rwlock.readLock().lock();
        MongoServer server= this.servers.search(addr);
        if (server != null) {
            return server;
        }
        return new MongoServer(addr, this.sync, this.dialer);
    }

    protected void addServer(MongoServer server, ServerInfo info, SyncKind syncKind)throws JmgoException {
        this.rwlock.writeLock().lock();
        MongoServer current = this.servers.search(server.getAddr());
        if(current == null){
            if(syncKind == SyncKind.PARTIAL_SYNC){
                //partialSync
                this.rwlock.writeLock().unlock();
                server.close();
                logger.info("SYNC Discarding unknown server {} due to partial sync", server.getAddr().getTcpaddr().toString());
                return;
            }
            this.servers.add(server);
            if(info.isMaster()){
                this.masters.add(server);
                logger.info("SYNC adding {} to cluster as a master", server.getAddr().getTcpaddr().toString());
            }else{
                logger.info("SYNC adding {} to cluster as a slave", server.getAddr().getTcpaddr().toString());
            }
        }else {
            if(server != current){
                throw new JmgoException("addServer attempting to add duplicated server");
            }
            if(server.getInfo().isMaster() != info.isMaster()){
                if(info.isMaster()){
                    logger.info("SYNC server {} is now a master", server.getAddr().getTcpaddr().toString());
                    this.masters.add(server);
                }else{
                    logger.info("SYNC server {} is now a slave", server.getAddr().getTcpaddr().toString());
                    this.masters.remove(server);
                }
            }
        }
        server.setInfo(info);
        logger.info("SYNC Broadcasting availability of server {}", server.getAddr().getTcpaddr().toString());
        this.serverSynced.signalAll();
        this.rwlock.writeLock().unlock();
    }

    private void syncServers() {
        this.sync.offer(true);
    }
}
