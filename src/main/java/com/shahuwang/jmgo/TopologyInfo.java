package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/2.
 */
public class TopologyInfo {
    private ServerInfo info;
    private ServerAddr[] hosts;

    public TopologyInfo(ServerInfo info, ServerAddr[] hosts){
        this.info = info;
        this.hosts = hosts;
    }

    public ServerInfo getInfo() {
        return info;
    }

    public ServerAddr[] getHosts() {
        return hosts;
    }
}
