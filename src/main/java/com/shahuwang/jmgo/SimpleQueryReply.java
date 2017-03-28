package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Created by rickey on 2017/3/28.
 */
public class SimpleQueryReply implements IReply{
    private Lock wait;
    private Lock change;
    private boolean replyDone;
    private List<Byte> replyData;
    private JmgoException replyErr;

    public SimpleQueryReply(Lock wait, Lock change, List<Byte> replyData){
        this.wait = wait;
        this.change = change;
        this.replyData = replyData;
    }

    public void reply(JmgoException e, ReplyOp reply, int docNum, byte[] docData){
        this.change.lock();
        if(!this.replyDone){
            this.replyDone = true;
            this.replyErr = e;
            if(e == null){
                
            }
        }
    }
}
