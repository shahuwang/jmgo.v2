package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/2/23.
 */
public class PoolLimitException extends JmgoException {
    private String message;
    public PoolLimitException(){
        this.message = "per-server connection limit reached";
    }
}
