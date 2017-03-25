package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/19.
 */
public class ReplyOp {
    private int flags;
    private long cursortId;
    private int firstDoc;
    private int replyDocs;
    public ReplyOp(int flags, long cursortId, int firstDoc, int replyDocs){
        this.firstDoc = firstDoc;
        this.flags = flags;
        this.cursortId = cursortId;
        this.replyDocs = replyDocs;
    }

    public int getReplyDocs() {
        return replyDocs;
    }

    @Override
    public String toString(){
        return String.format("flags is %d, cursorId is %d, firstDocs is %d, replyDocs is %d", this.flags, this.cursortId, this.firstDoc, this.replyDocs);
    }
}
