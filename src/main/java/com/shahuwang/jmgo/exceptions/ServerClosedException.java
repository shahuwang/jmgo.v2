package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/2/23.
 */
public class ServerClosedException extends JmgoException {
    private String message;
    public ServerClosedException(){
        this.message = "server was closed";
    }
}
