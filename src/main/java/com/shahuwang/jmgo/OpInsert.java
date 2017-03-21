package com.shahuwang.jmgo;

import org.bson.BsonDocument;

import java.util.List;

/**
 * Created by rickey on 2017/3/21.
 */
public class OpInsert implements IOperator{
    private String collection; //dbname.colname
    private List<BsonDocument> documents;
    private int flags;
    public OpInsert(String collection, List<BsonDocument> documents, int flags){
        this.collection = collection;
        this.documents = documents;
        this.flags = flags;
    }

    public int getFlags() {
        return flags;
    }

    public String getCollection() {
        return collection;
    }

    public List<BsonDocument> getDocuments() {
        return documents;
    }

    public OpCode getOpCode(){
        return OpCode.OP_INSERT;
    }
}
