package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/3/2.
 */
public class SyncServerException extends JmgoException {
    private String message;
    public SyncServerException(String message){
        this.message = message;
    }
}
