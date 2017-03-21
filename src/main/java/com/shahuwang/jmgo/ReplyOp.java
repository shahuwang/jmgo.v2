package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/19.
 */
public class ReplyOp {
    private int flags;
    private int cursortId;
    private int firstDoc;
    private int replyDocs;
    public ReplyOp(int flags, int cursortId, int firstDoc, int replyDocs){
        this.firstDoc = firstDoc;
        this.flags = flags;
        this.cursortId = cursortId;
        this.firstDoc = firstDoc;
    }

    public int getReplyDocs() {
        return replyDocs;
    }
}
