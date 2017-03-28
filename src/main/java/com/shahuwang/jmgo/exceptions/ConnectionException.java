package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/2/27.
 */
public class ConnectionException extends JmgoException {
    private String message;
    public ConnectionException(String message){
        this.message = message;
    }
}
