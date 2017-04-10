package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.SyncServerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rickey on 2017/3/11.
 */
public class SpawnSync implements Runnable {
    protected static Map<ServerAddr, PendingAdd> notYetAdd = new HashMap<>();
    protected static Map<ServerAddr, Boolean>addIfFound = new HashMap<>();
    protected static Map<ServerAddr, Boolean>seen = new HashMap<>();
    protected static SyncKind syncKind = SyncKind.PARTIAL_SYNC;
    private static ReentrantLock lock = new ReentrantLock();
    private ServerAddr addr;
    private boolean byMaster;
    private MongoCluster cluster;
    private boolean direct;
    private Phaser phaser;
    Logger logger = LogManager.getLogger(SpawnSync.class.getName());

    public SpawnSync(MongoCluster cluster, ServerAddr addr, boolean byMaster, boolean direct, Phaser phaser) {
        this.cluster = cluster;
        this.addr = addr;
        this.byMaster = byMaster;
        this.direct = direct;
        this.phaser = phaser;
    }

    public void run() {
        this.phaser.register();
        lock.lock();
        if(this.byMaster) {
            PendingAdd pending = notYetAdd.get(this.addr);
            if (pending != null) {
                notYetAdd.remove(this.addr);
                lock.unlock();
                try {
                    this.cluster.addServer(pending.server, pending.info, SyncKind.COMPLETE_SYNC);
                }catch (JmgoException e){
                    logger.catching(e);
                    this.phaser.arrive();
                    return;
                }
                addIfFound.put(this.addr, new Boolean(true));
            }
        }
        if (seen.containsKey(this.addr)) {
            lock.unlock();
            this.phaser.arrive();
            return;
        }
        seen.put(this.addr, new Boolean(true));
        lock.unlock();

        MongoServer server = this.cluster.server(this.addr);
        TopologyInfo tpinfo;
        try {
            tpinfo = this.cluster.syncServer(server);
        }catch (SyncServerException e){
            this.cluster.removeServer(server);
            this.phaser.arrive();
            return;
        }
        lock.lock();
        boolean add = this.direct || tpinfo.getInfo().isMaster() || addIfFound.containsKey(this.addr);
        if (add) {
            syncKind = SyncKind.COMPLETE_SYNC;
        } else {
            notYetAdd.put(this.addr, new PendingAdd(server, tpinfo.getInfo()));
        }
        lock.unlock();
        if (add) {
            try {
                this.cluster.addServer(server, tpinfo.getInfo(), SyncKind.COMPLETE_SYNC);
            }catch (JmgoException e){
                logger.catching(e);
            }
        }
        if (!this.direct) {
            for (ServerAddr addr: tpinfo.getHosts()) {
                Thread thread = new Thread(new SpawnSync(this.cluster, addr, tpinfo.getInfo().isMaster(), this.direct, phaser));
                thread.start();
            }
        }
        this.phaser.arrive();
    }


}
