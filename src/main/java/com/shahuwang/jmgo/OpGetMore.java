package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/26.
 */
public class OpGetMore implements IOperator{
    private String collection;
    private int limit;
    private long cursorId;
    private IReply replyFunc;

    public OpCode getOpCode(){
        return OpCode.OP_GET_MORE;
    }

    public String getCollection() {
        return collection;
    }

    public OpGetMore setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public OpGetMore setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public long getCursorId() {
        return cursorId;
    }

    public OpGetMore setCursorId(long cursorId) {
        this.cursorId = cursorId;
        return this;
    }

    public IReply getReplyFunc() {
        return replyFunc;
    }

    public OpGetMore setReplyFunc(IReply replyFunc) {
        this.replyFunc = replyFunc;
        return this;
    }
}
