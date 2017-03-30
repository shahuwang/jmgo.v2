package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Created by rickey on 2017/3/28.
 */
public class SimpleQueryReply implements IReply{
    private SyncChan<List<Byte>> wait;
    private Lock change;
    private boolean replyDone;
    private List<Byte> replyData;
    private JmgoException replyErr;
    private Logger logger = Log.getLogger(SimpleQueryReply.class.getName());
    public SimpleQueryReply(SyncChan<List<Byte>> wait, Lock change){
        this.wait = wait;
        this.change = change;
        this.replyData = new ArrayList<>();
    }

    public void reply(JmgoException e, ReplyOp reply, int docNum, byte[] docData){
        this.change.lock();
        logger.debug("Simple query get response: {}, {}, {}", e, reply, docNum);
        if(!this.replyDone){
            this.replyDone = true;
            this.replyErr = e;
            if(e == null){
                for(byte b: docData){
                    this.replyData.add(new Byte(b));
                }
            }
        }
        this.change.unlock();
        this.wait.put(this.replyData);
    }

    public JmgoException getReplyErr() {
        return replyErr;
    }
}
