package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/2/23.
 */
public class JmgoException extends Exception{
    private String message;

    public JmgoException() {
        this.message = "";
    }

    public JmgoException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
