package com.shahuwang.jmgo;


import com.shahuwang.jmgo.exceptions.*;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/3/22.
 */
public class MongoServer {
    private ServerAddr addr;
    private List<MongoSocket> unusedSockets = new ArrayList<>();
    private List<MongoSocket> liveSockets = new ArrayList<>();
    private boolean closed;
    private boolean abended;
    private IDialer dialer;
    private SyncChan<Boolean> sync;
    private Duration pingValue;
    private int pingIndex;
    private int pingCount;
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private ServerInfo info = new ServerInfo(false, false, null,  "", 0);
    private Duration[] pingWindow = new Duration[6];
    public static final Duration pingDelay = Duration.ofSeconds(15);

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

    public ServerInfo getInfo() {
        return info;
    }

    public MongoSocket acquireSocket(int poolLimit, Duration timeout)throws SocketAbendException, PoolLimitException, ServerClosedException{
        while (true){
            this.rwlock.writeLock().lock();
            MongoSocket socket = null;
            boolean abended = this.abended;
            if(this.closed){
                this.rwlock.writeLock().unlock();
                if(abended){
                    throw new SocketAbendException();
                }
                return null;
            }

            int n = this.unusedSockets.size();
            // live - unused 等于当前正在使用的socket数量
            if(poolLimit > 0 && this.liveSockets.size() - n >= poolLimit){
                this.rwlock.writeLock().unlock();
                throw new PoolLimitException();
            }

            if(n > 0){
                socket = this.unusedSockets.get(n - 1);
                this.unusedSockets.remove(n - 1);
                ServerInfo info = this.info;
                this.rwlock.writeLock().unlock();
                try {
                    socket.initialAcquire(info, timeout);
                }catch (JmgoException e){
                    logger.catching(e);
                    continue;
                }
            }else{
                this.rwlock.writeLock().unlock();
                try{
                    socket = this.connect(timeout);
                    this.rwlock.writeLock().lock();
                    if(this.closed){
                        this.rwlock.writeLock().unlock();
                        socket.release();
                        socket.close();
                        if(abended){
                            throw new SocketAbendException();
                        }
                        throw new ServerClosedException();
                    }
                    this.liveSockets.add(socket);
                    this.rwlock.writeLock().unlock();
                }catch (JmgoException e){
                    logger.catching(e);
                }
            }
            return socket;
        }
    }

    public MongoSocket connect(Duration timeout)throws ConnectionException{
        this.rwlock.readLock().lock();
        boolean ismaster = this.info.isMaster();
        IDialer dial = this.dialer;
        this.rwlock.readLock().unlock();
        logger.info("Establishing new connection to {} (timeout={})...", this.addr, timeout);
        Socket conn;
        try{
            conn = dialer.dial(this.addr, timeout);
        }catch (JmgoException e){
            logger.error("Connection to {} failed: {}", this.addr.getTcpaddr(), e);
            throw new ConnectionException(e.getMessage());
        }
        logger.info("Connection to {} established", this.addr.getTcpaddr());
        Stats.getInstance().setConn(1, ismaster);
        return new MongoSocket(this, conn, timeout);
    }

    public void recycleSocket(MongoSocket socket){
        this.rwlock.writeLock().lock();
        if(!this.closed){
            this.unusedSockets.add(socket);
        }
        this.rwlock.writeLock().unlock();
    }

    protected boolean hasTags(BsonDocument serverTags){
        BsonDocument tags = this.info.getTags();
        for(Map.Entry<String, BsonValue> tag: serverTags.entrySet()){
             BsonValue value = tag.getValue();
             String key = tag.getKey();
             if(tags.containsKey(key)){
                 BsonValue originValue = tags.get(key);
                 if(!value.equals(originValue)){
                     return false;
                 }
             }else{
                 return false;
             }
        }
        return true;
    }

    public void setInfo(ServerInfo info){
        this.rwlock.writeLock().lock();
        this.info = info;
        this.rwlock.writeLock().unlock();

    }

    public void close(){
        this.rwlock.writeLock().lock();
        this.closed = true;
        List<MongoSocket> lilveSockets = this.liveSockets;
        List<MongoSocket> unusedSockets = this.unusedSockets;
        this.liveSockets = new ArrayList<>();
        this.unusedSockets = new ArrayList<>();
        this.rwlock.writeLock().unlock();
        for(MongoSocket s: lilveSockets){
            s.close();
        }
        lilveSockets.clear();
        unusedSockets.clear();
    }

    protected void pinger(boolean loop){
        Duration delay;
        boolean racedetector = BuildConfig.getInstance().getRacedetector();
        if (racedetector) {
            synchronized (GlobalMutex.class){
                delay = this.pingDelay;
            }
        }else {
            delay = this.pingDelay;
        }
        BsonDocument query = new BsonDocument("ping", new BsonInt32(1));
        OpQuery op = new OpQuery();
        op.setCollection("admin.$cmd").setQuery(query).setFlags(OpQueryFlag.flagSlaveOk).setLimit(-1);
        while (true){
            if(loop){
                try{
                    TimeUnit.SECONDS.sleep(delay.getSeconds());
                }catch (InterruptedException e){
                    logger.error(e.getMessage());
                }

                try{
                    MongoSocket socket = this.acquireSocket(0, delay);
                    long start = System.currentTimeMillis();
                    socket.simpleQuery(op);
                    long end = System.currentTimeMillis();
                    Duration delay2 = Duration.ofMillis(end - start);
                    this.pingWindow[this.pingIndex] = delay2;
                    this.pingIndex  = (this.pingIndex + 1) % this.pingWindow.length;
                    this.pingCount++;
                    Duration max = Duration.ofSeconds(0);
                    for(int i=0; i<this.pingWindow.length && i<this.pingCount;i++){
                        if(this.pingWindow[i].compareTo(max) > 0){
                            max = this.pingWindow[i];
                        }
                    }
                    socket.release();
                    this.rwlock.writeLock().lock();;
                    if(this.closed){
                        loop=false;
                    }
                    this.pingValue = max;
                    this.rwlock.writeLock().unlock();

                }catch (JmgoException e){
                    logger.debug("pinger get an exception {}", e);
                }
                if(!loop){
                    return;
                }
            }
        }
    }
}
