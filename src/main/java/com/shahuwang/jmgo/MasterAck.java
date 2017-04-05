package com.shahuwang.jmgo;

import org.bson.BsonElement;

/**
 * Created by rickey on 2017/3/2.
 */
public class MasterAck {
    private boolean isMaster;
    private boolean secondary;
    private ServerAddr primary;
    private ServerAddr[] hosts;
    private ServerAddr[] passives;
    private BsonElement[]Tags;
    private String setName;
    private String msg;
    private int maxWireVersion;

    public boolean isMaster() {
        return isMaster;
    }

    public boolean isSecondary() {
        return secondary;
    }

    public ServerAddr getPrimary() {
        return primary;
    }

    public ServerAddr[] getHosts() {
        return hosts;
    }

    public ServerAddr[] getPassives() {
        return passives;
    }

    public String getSetName() {
        return setName;
    }

    public int getMaxWireVersion() {
        return maxWireVersion;
    }

    public BsonElement[] getTags() {
        return Tags;
    }

    public String getMsg() {
        return msg;
    }
}
