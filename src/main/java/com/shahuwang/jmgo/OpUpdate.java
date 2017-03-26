package com.shahuwang.jmgo;

import org.bson.BsonBoolean;
import org.bson.BsonDocument;

/**
 * Created by rickey on 2017/3/26.
 */
public class OpUpdate implements IOperator{
    private String collection;
    private BsonDocument selector;
    private BsonDocument update;
    private int flags;
    private boolean multi;
    private boolean upsert;

    public BsonDocument getDocument(){
        BsonDocument doc = new BsonDocument();
        doc.append("q", this.selector);
        doc.append("u", this.update);
        if(this.multi){
            doc.append("multi", new BsonBoolean(this.multi));
        }
        if(this.upsert){
            doc.append("upsert", new BsonBoolean(this.upsert));
        }
        return doc;
    }

    public OpCode getOpCode(){
        return OpCode.OP_UPDATE;
    }

    public OpUpdate setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public OpUpdate setSelector(BsonDocument selector) {
        this.selector = selector;
        return this;
    }

    public OpUpdate setUpdate(BsonDocument update) {
        this.update = update;
        return this;
    }

    public OpUpdate setFlags(int flags) {
        this.flags = flags;
        return this;
    }

    public OpUpdate setMulti(boolean multi) {
        this.multi = multi;
        return this;
    }

    public OpUpdate setUpsert(boolean upsert) {
        this.upsert = upsert;
        return this;
    }

    public BsonDocument getUpdate() {
        return update;
    }

    public String getCollection() {
        return collection;
    }

    public int getFlags() {
        return flags;
    }

    public BsonDocument getSelector() {
        return selector;
    }



}
