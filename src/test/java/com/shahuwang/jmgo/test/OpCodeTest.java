package com.shahuwang.jmgo.test;

import com.shahuwang.jmgo.*;
import com.shahuwang.jmgo.exceptions.WriteIOException;
import junit.framework.TestCase;
import org.bson.BSON;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rickey on 2017/3/21.
 */
public class OpCodeTest extends TestCase{
    public void testEnum(){
        OpCode code = OpCode.OP_COMMAND;
        System.out.println(code.getCode());
        System.out.println((byte)1);
        System.out.println("s".getBytes());
    }

    public void testInsert(){
        Socket conn = null;
        try {
            conn = new Socket("127.0.0.1", 27017);
        }catch (IOException e){
            Log.getLogger(OpCodeTest.class.getName()).catching(e);
        }
        MongoSocket mso = new MongoSocket(null, conn, null);
        BsonDocument doc = new BsonDocument("a", new BsonString("hello world"));
        List<BsonDocument> docs = new ArrayList<>();
        docs.add(doc);
        IOperator op = new OpInsert("test.test", docs, 0);
        try {
            mso.Query(op);
        }catch (WriteIOException e){
            System.out.println(e.getMessage());
        }
    }
}