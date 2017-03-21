package com.shahuwang.jmgo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by rickey on 2017/2/27.
 */
public class Stats {
    private int clusters = 0;
    private int masterConns = 0;
    private int slaveConns = 0;
    private int sentOps = 0;
    private int receivedOps = 0;
    private int receivedDocs = 0;
    private int socketsAlive = 0;
    private int socketsInUse = 0;
    private int socketRefs = 0;
    private boolean enabled = true;
    private static Stats ourInstance = new Stats();

    public static Stats getInstance() {
        return ourInstance;
    }
    Logger logger = LogManager.getLogger(Stats.class.getName());

    private Stats() {
    }

    public synchronized void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    public synchronized void resetStats() {
        logger.info("Resetting stats");
        Stats instance = new Stats();
        instance.clusters = this.ourInstance.clusters;
        instance.socketsInUse = this.ourInstance.socketsInUse;
        instance.socketsAlive = this.ourInstance.socketsAlive;
        instance.socketRefs = this.socketRefs;
        this.ourInstance = instance;
    }

    public synchronized void setCluster(int delta){
        if (this.enabled){
            this.clusters += delta;
        }
    }

    public synchronized void setConn(int delta, boolean master){
        if (this.enabled){
            if (master) {
                this.masterConns += delta;
            }else{
                this.slaveConns += delta;
            }
        }
    }

    public synchronized void setSentOps(int delta){
        if (this.enabled) {
            this.sentOps += delta;
        }
    }

    public synchronized void setReceivedOps(int delta){
        if (this.enabled){
            this.receivedOps += delta;
        }
    }

    public synchronized void setReceivedDocs(int delta){
        if (this.enabled) {
            this.receivedDocs += delta;
        }
    }

    public synchronized void setSocketsInUse(int delta){
        if(this.enabled){
            this.socketsInUse += delta;
        }
    }

    public synchronized void setSocketsAlive(int delta){
        if(this.enabled){
            this.socketsAlive += delta;
        }
    }

    public synchronized void setSocketRefs(int delta){
        if (this.enabled){
            this.socketRefs += delta;
        }
    }

}
