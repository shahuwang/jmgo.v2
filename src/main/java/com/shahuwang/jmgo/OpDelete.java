package com.shahuwang.jmgo;

import org.bson.BsonDocument;
import org.bson.BsonInt32;

/**
 * Created by rickey on 2017/3/26.
 */
public class OpDelete implements IOperator{
    private String collection;
    private BsonDocument selector;
    private int flags;
    private int limit;

    public BsonDocument getDocument(){
       BsonDocument doc = new BsonDocument("q", this.selector);
       doc.append("limit", new BsonInt32(this.limit));
       return doc;
    }

    public OpCode getOpCode(){
        return OpCode.OP_DELETE;
    }

    public String getCollection() {
        return collection;
    }

    public OpDelete setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public BsonDocument getSelector() {
        return selector;
    }

    public OpDelete setSelector(BsonDocument selector) {
        this.selector = selector;
        return this;
    }

    public int getFlags() {
        return flags;
    }

    public OpDelete setFlags(int flags) {
        this.flags = flags;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public OpDelete setLimit(int limit) {
        this.limit = limit;
        return this;
    }
}
