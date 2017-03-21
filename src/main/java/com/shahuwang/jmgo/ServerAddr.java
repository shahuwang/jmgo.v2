package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;

import java.net.InetSocketAddress;

/**
 * Created by rickey on 2017/2/23.
 */
public class ServerAddr {
    private String addr_str;
    private InetSocketAddress tcpaddr;
    public ServerAddr(String addr) throws JmgoException {
        // 类似 127.0.0.1:27017 或者 127.0.0.1，使用默认端口
        this.addr_str = addr;
        String [] sp = addr.split(":");
        if(sp.length > 2) {
            throw new JmgoException("Invalid server address");
        }
        String host = sp[0];
        int port = 27017;
        if (sp.length > 0) {
            port = (int)Integer.parseInt(sp[1]);
        }

        this.tcpaddr = new InetSocketAddress(host,port);
    }

    public ServerAddr(String host, int port) {
        this.tcpaddr = new InetSocketAddress(host, port);
        this.addr_str = String.format("%s:%d", host, port);
    }

    public InetSocketAddress getTcpaddr() {
        return tcpaddr;
    }

    public String toString() {
        return this.addr_str;
    }
}
