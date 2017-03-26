package com.shahuwang.jmgo.test;

import com.shahuwang.jmgo.*;
import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.SocketDeadException;
import com.shahuwang.jmgo.exceptions.WriteIOException;
import junit.framework.TestCase;
import org.apache.logging.log4j.Logger;
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
    private Logger logger = Log.getLogger(MainTest.class.getName());
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
        BsonDocument doc = new BsonDocument("ab", new BsonString("hello world"));
        List<BsonDocument> docs = new ArrayList<>();
        docs.add(doc);
        IOperator op = new OpInsert("test.test", docs, 0);
        Socket conn = buildConn();
        writeQuery(conn, op);
    }

    public void testQuery(){
        OpQuery query = new OpQuery();
        query.setCollection("test.test")
                .setQuery(new BsonDocument("ab", new BsonString("hello world")));
        IReply reply = (JmgoException e, ReplyOp rop, int docNum, byte[]docData) -> {
            logger.debug("doc num is {}", docNum);
            logger.debug("reply op is {}", rop);
        };
        query.setReplyFuncs(reply);
        Socket conn = buildConn();
        writeQuery(conn, query);
    }

    public void testUpdate(){
        OpUpdate op = new OpUpdate();
        BsonDocument selector = new BsonDocument("ab", new BsonString("hello world"));
        BsonDocument updator = new BsonDocument("$set", new BsonDocument("ab", new BsonString("你好")));
        op.setCollection("test.test")
                .setSelector(selector)
                .setUpdate(updator);
        Socket conn = buildConn();
        writeQuery(conn, op);

    }

    public void testDelete(){
        BsonDocument doc = new BsonDocument("delete", new BsonString("to delete"));
        List<BsonDocument> docs = new ArrayList<>();
        docs.add(doc);
        IOperator op = new OpInsert("test.test", docs, 0);
        OpDelete delete = new OpDelete();
        delete.setCollection("test.test")
                .setFlags(0)
                .setSelector(doc);
        Socket conn = buildConn();
        writeQuery(conn, op, delete);
    }

    private Socket buildConn(){
        Socket conn = null;
        try {
            conn = new Socket("127.0.0.1", 27017);
        }catch (IOException e){
            logger.catching(e);
        }
        return conn;
    }

    private void writeQuery(Socket conn, IOperator ...ops){
        MongoSocket mso = new MongoSocket(null, conn, null);
        try {
            mso.Query(ops);
        }catch (WriteIOException| SocketDeadException e){
            logger.catching(e);
        }
        try {
            conn.close();
        }catch (IOException e){
            logger.catching(e);
        }

    }
}
