package com.shahuwang.jmgo.test;

import com.shahuwang.jmgo.*;
import com.shahuwang.jmgo.exceptions.WriteIOException;
import junit.framework.TestCase;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by rickey on 2017/3/21.
 */
public class MainTest extends TestCase{
    public void testEnum(){
        OpCode code = OpCode.OP_COMMAND;
        System.out.println(code.getCode());
        int b = 10234;
        byte[] buf = new byte[4];
        buf[0] = (byte)(b);
        buf[1] = (byte)(b>>8);
        buf[2] = (byte)(b>>16);
        buf[3] = (byte)(b>>24);
        System.out.println(Arrays.toString(buf));
        byte[] buf2 = ByteBuffer.allocate(4).putInt(b).array();
        System.out.println(Arrays.toString(buf2));
        System.out.println(-6 & 0xff);
        System.out.println((int)buf[0]);

    }

    public void testInsert(){
        Socket conn = null;
        try {
            conn = new Socket("127.0.0.1", 27017);
        }catch (IOException e){
            Log.getLogger(MainTest.class.getName()).catching(e);
        }
        MongoSocket mso = new MongoSocket(null, conn, null);
        BsonDocument doc = new BsonDocument("ab", new BsonString("hello world"));
        List<BsonDocument> docs = new ArrayList<>();
        docs.add(doc);
        IOperator op = new OpInsert("test.test", docs, 0);
        try {
            mso.Query(op);
        }catch (WriteIOException e){
            System.out.println(e.getMessage());
        }
        try{
            Thread.sleep(10000);
            conn.close();
        }catch (IOException | InterruptedException e){
            System.out.println(e.getMessage());
        }
    }
}
