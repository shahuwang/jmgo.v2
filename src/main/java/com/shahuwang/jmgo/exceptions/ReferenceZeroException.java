package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/3/2.
 */
public class ReferenceZeroException extends JmgoException {
    private String message;
    public ReferenceZeroException(String message){
        this.message = message;
    }
}
