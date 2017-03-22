package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.WriteIOException;
import org.apache.logging.log4j.Logger;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by rickey on 2017/3/21.
 */
public class MongoSocket {
    private ServerAddr addr;
    private JmgoException dead = null;
    private Map<Integer, IReply> replyFuncs = new HashMap<>();
    private int nextRequestId;
    private Lock lock = new ReentrantLock();
    private Socket conn;
    private MongoServer server;
    private Duration timeout;
    private Condition getNonce;
    private Byte[] emptyHeader ={0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private Logger logger = Log.getLogger(MongoSocket.class.getName());
    private static Codec<BsonDocument> DocumentCodec = new BsonDocumentCodec();

    public MongoSocket(MongoServer server, Socket conn, Duration timeout){
        this.conn = conn;
        this.server = server;
        this.getNonce = this.lock.newCondition();
    }

    public void Query(IOperator ...ops)throws WriteIOException{
        List<IOperator> lops = Arrays.asList(ops);
        List<IOperator> fops = this.flushLogout();
        if(fops.size() > 0){
            lops.addAll(fops);
        }
        List<Byte> buf = new ArrayList<Byte>(256);
        List<requestInfo> requests = new ArrayList<>(lops.size());
        int requestCount = 0;
        for(IOperator op: lops){
            logger.debug("Socket {} to {}： serializing op: {}", this.addr, op);
            if(op instanceof OpQuery){
                //TODO
            }
            int start = buf.size();
            IReply replyFunc = null;
            if(op instanceof OpInsert){
                buf = addHeader(buf, op.getOpCode());
                buf = addInt(buf, ((OpInsert) op).getFlags());
                buf = addCString(buf, ((OpInsert) op).getCollection());
                for(BsonDocument doc: ((OpInsert) op).getDocuments()){
                    logger.debug("Socket {} to {}: serializing document for insertion: {}", this, this.addr, doc);
                    buf = addBSON(buf, doc);
                }
            }
            //TODO

            setInt(buf, start, buf.size() - start);
            if (replyFunc != null){
                //TODO
            }

        }
        this.lock.lock();
        if(this.dead != null){
            //TODO
        }
        boolean wasWaiting = this.replyFuncs.isEmpty();
        int requestId = this.nextRequestId + 1;
        if(requestId == 0){
            // 0 是没有response的
            requestId++;
        }
        this.nextRequestId = requestId + requestCount;
        for(int i=0; i != requestCount; i++){
            requestInfo request = requests.get(i);
            setInt(buf, request.bufferPos+4, requestId);
            this.replyFuncs.put(new Integer(requestId), request.replyFunc);
            requestId++;
        }
        logger.debug("Socket {} to {}: sending {} op(s) ({} bytes)", this, this.addr, lops.size(), buf.size());
        Stats.getInstance().setSentOps(lops.size());
        updateDeadline(deadlineType.WRITE_DEAD_LINE);
        try{
            this.conn.getOutputStream().write(iByteToArray(buf));
        }catch (IOException e){
            if(!wasWaiting && requestCount > 0){
                this.updateDeadline(deadlineType.READ_DEAD_LINE);
            }
            this.lock.unlock();
            throw new WriteIOException(e.getMessage());
        }
        this.lock.unlock();


    }

    private void updateDeadline(deadlineType which){
        //TODO
    }

    private List<IOperator> flushLogout() {
        //TODO
        return new ArrayList<>();
    }

    private byte[] iByteToArray(List<Byte> buf){
        byte[] b = new byte[buf.size()];
        for(int i=0; i<buf.size(); i++){
            b[i] = buf.get(i).byteValue();
        }
        return b;
    }

    private List<Byte> addHeader(List<Byte> b, OpCode opCode){
        int len = b.size();
        b.addAll(Arrays.asList(emptyHeader));
        int code = opCode.getCode();
        // 目前的opcode数值比较小，两位就足够了
        b.set(len+12, (byte)code);
        b.set(len+13, (byte)(code >> 8));
        return b;
    }
    private List<Byte> addInt(List<Byte> b, int i){
        b.add((byte)i);
        b.add((byte)(i>>8));
        b.add((byte)(i>>16));
        b.add((byte)(i>>24));
        return b;
    }

    private void setInt(List<Byte> b, int pos, int i){
        b.set(pos, new Byte((byte)i));
        b.set(pos+1, new Byte((byte)(i>>8)));
        b.set(pos+2, new Byte((byte)(i>>16)));
        b.set(pos+3, new Byte((byte)(i>>24)));
    }

    private List<Byte>addLong(List<Byte> b, long i){
        b.add((byte)i);
        b.add((byte)(i>>8));
        b.add((byte)(i>>16));
        b.add((byte)(i>>24));
        b.add((byte)(i>>32));
        b.add((byte)(i>>40));
        b.add((byte)(i>>48));
        b.add((byte)(i>>56));
        return b;
    }

    private List<Byte>addBSON(List<Byte>b, BsonDocument doc){
        if(doc == null){
            Byte[] empty = {5, 0, 0, 0, 0};
            b.addAll(Arrays.asList(empty));
            return b;
        }

        BasicOutputBuffer buff = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buff);
        DocumentCodec.encode(writer, doc, EncoderContext.builder().build());
        for(byte i: buff.toByteArray()){
            b.add(new Byte(i));
        }
        return b;
    }

    private List<Byte> addCString(List<Byte> b, String s){
        for(byte i: s.getBytes()){
            b.add(new Byte(i));
        }
        b.add(new Byte((byte)0));
        return b;
    }

    private class requestInfo{
        private int bufferPos;
        private IReply replyFunc;
    }

    private enum deadlineType{
        READ_DEAD_LINE,
        WRITE_DEAD_LINE;
    }

}
