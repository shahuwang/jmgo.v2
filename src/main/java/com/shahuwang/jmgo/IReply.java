package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;

/**
 * Created by rickey on 2017/2/28.
 */
@FunctionalInterface
public interface IReply {
    // TODO
    public void reply(JmgoException error, ReplyOp reply, int docNum, byte[] docData);
}
