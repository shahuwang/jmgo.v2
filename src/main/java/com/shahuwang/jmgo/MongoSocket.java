package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.SocketDeadException;
import com.shahuwang.jmgo.exceptions.WriteIOException;
import org.apache.logging.log4j.Logger;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
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
    private SocketDeadException dead = null;
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
    private ServerInfo serverInfo;
    private List<Credential> creds;
    private List<Credential> logout;

    public MongoSocket(MongoServer server, Socket conn, Duration timeout){
        this.conn = conn;
        this.server = server;
        this.getNonce = this.lock.newCondition();
        new Thread(() -> readLoop()).start();
    }

    public void login(Credential cred){
        this.lock.lock();
        if(cred.getMechanism() == "" && this.serverInfo.getMaxWireVersion() >= 3){
            cred.setMechanism("SCRAM-SHA-1"); // 默认的机制
        }
        for(Credential socketCred: this.creds){
            if(socketCred == cred){
                logger.debug("Socket {} to {}: login: db={}, user={} (already logged in)", this, this.addr, cred.getSource(), cred.getUsername());
                this.lock.unlock();
                return;
            }
        }
        if(this.dropLogout(cred)){
            logger.debug("Socket {} to {}: login: db={}, user={} (cached)", this, this.addr, cred.getSource(), cred.getUsername());
            this.creds.add(cred);
            this.lock.unlock();
            return;
        }
        this.lock.unlock();
        logger.debug("Socket {} to {}: login: db={}, user={} (cached)", this, this.addr, cred.getSource(), cred.getUsername());
        String mech = cred.getMechanism();
        if(mech == "" || mech == "MONGODB-CR" || mech=="MONGO-CR"){
            // mongo 3.0 版之前的默认认证方法
            this.loginClassic(cred);
        } else if(mech == "PLAIN") {
            // 商业版的ldap，使用SASL的plain模式
            this.loginPlain(cred);
        } else if(mech == "MONGODB-X509"){
            this.loginX509(cred);
        }else{
            this.loginSASL(cred);
        }
    }

    public void Query(IOperator ...ops)throws WriteIOException, SocketDeadException{
        List<IOperator> lops = Arrays.asList(ops);
        List<IOperator> fops = this.flushLogout();
        if(fops.size() > 0){
            lops.addAll(fops);
        }
        List<Byte> buf = new ArrayList<Byte>(256);
        requestInfo[] requests = new requestInfo[lops.size()];
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
            } else if (op instanceof OpQuery){
                buf = addHeader(buf, op.getOpCode());
                buf = addInt(buf, ((OpQuery) op).getFlags());
                buf = addCString(buf, ((OpQuery) op).getCllection());
                buf = addInt(buf, ((OpQuery) op).getSkip());
                buf = addInt(buf, ((OpQuery) op).getLimit());
                buf = addBSON(buf, ((OpQuery) op).finalQuery(this));
                if(((OpQuery) op).getSelector() != null){
                    buf = addBSON(buf, ((OpQuery) op).getSelector());
                }
                replyFunc = ((OpQuery) op).getReplyFunc();
            } else if(op instanceof OpUpdate){
                buf = addHeader(buf, op.getOpCode());
                buf = addInt(buf, 0);
                buf = addCString(buf, ((OpUpdate) op).getCollection());
                buf = addInt(buf, ((OpUpdate) op).getFlags());
                buf = addBSON(buf, ((OpUpdate) op).getSelector());
                buf = addBSON(buf, ((OpUpdate) op).getUpdate());
            } else if(op instanceof OpDelete){
                buf = addHeader(buf, op.getOpCode());
                buf = addInt(buf, 0);
                buf = addCString(buf, ((OpDelete) op).getCollection());
                buf = addInt(buf, ((OpDelete) op).getFlags());
                buf = addBSON(buf, ((OpDelete) op).getSelector());
            } else if(op instanceof OpGetMore){
                buf = addHeader(buf, op.getOpCode());
                buf = addInt(buf, 0);
                buf = addCString(buf, ((OpGetMore) op).getCollection());
                buf = addInt(buf, ((OpGetMore) op).getLimit());
                buf = addLong(buf, ((OpGetMore) op).getCursorId());
                replyFunc = ((OpGetMore) op).getReplyFunc();
            } else if(op instanceof OpKillCursors){
                buf = addHeader(buf, op.getOpCode());
                buf = addInt(buf, 0);
                buf = addInt(buf, ((OpKillCursors) op).getCursorIds().length);
                for(long cursorid: ((OpKillCursors) op).getCursorIds()){
                    buf = addLong(buf, cursorid);
                }
            }else{
                throw new RuntimeException("internal error: unknown operation type");
            }

            setInt(buf, start, buf.size() - start);
            if (replyFunc != null){
                requestInfo request = new requestInfo();
                request.replyFunc = replyFunc;
                request.bufferPos = start;
                requests[requestCount] = request;
                requestCount++;
            }

        }
        this.lock.lock();
        if(this.dead != null){
            JmgoException dead = this.dead;
            this.lock.unlock();
            for(int i=0; i!=requestCount; i++){
                requestInfo request = requests[i];
                if(request.replyFunc != null){
                    request.replyFunc.reply(dead, null, -1, null);
                }
            }
            throw this.dead;
        }
        boolean wasWaiting = this.replyFuncs.isEmpty();
        int requestId = this.nextRequestId + 1;
        if(requestId == 0){
            // 0 是没有response的
            requestId++;
        }
        this.nextRequestId = requestId + requestCount;
        for(int i=0; i != requestCount; i++){
            requestInfo request = requests[i];
            setInt(buf, request.bufferPos+4, requestId);
            this.replyFuncs.put(new Integer(requestId), request.replyFunc);
            requestId++;
        }
        logger.debug("Socket {} to {}: sending {} op(s) ({} bytes)", this, this.addr, lops.size(), buf.size());
        Stats.getInstance().setSentOps(lops.size());
        updateDeadline();
        try{
            this.conn.getOutputStream().write(iByteToArray(buf));
        }catch (IOException e){
            if(!wasWaiting && requestCount > 0){
                this.updateDeadline();
            }
            this.lock.unlock();
            throw new WriteIOException(e.getMessage());
        }
        this.lock.unlock();


    }

    public ServerInfo getServerInfo() {
        this.lock.lock();
        ServerInfo info = this.serverInfo;
        this.lock.unlock();
        return info;
    }

    public void  close(){
        this.kill(new JmgoException("Closed explicitly"), false);
    }

    private void loginClassic(Credential cred){
        //TODO
    }

    private void loginPlain(Credential cred){
        //TODO
    }

    private void loginX509(Credential cred){
        //TODO
    }

    private void loginSASL(Credential cred){
        if(cred.getMechanism() == "SCRAM-SHA-1"){

        }
    }

    private void readLoop(){
        byte[] p = new byte[36]; // 固定的header占16 byte，固定的其他字段占20 byte
        byte[] s = new byte[4]; // 用于读取bson的头部，指明了该bson数据长度是多少
        Socket conn = this.conn;
        while (true){
            try {
                fill(conn, p);
            }catch (IOException e){
                kill(new JmgoException(e.getMessage()), true);
                return;
            }
            //要注意，数据是小端存储的
            int totalLen = getInt(p, 0);
            int responseTo = getInt(p, 8);
            int opCode = getInt(p, 12); // 目前的opcode只需要2 byte
            if(opCode != 1){
                kill(new JmgoException("opcode != 1, corrupted data?"), true);
                return;
            }
            int flags = getInt(p, 16) & 0xff; // 转换成unsigned int
            ReplyOp reply = new ReplyOp(
                    flags, getLong(p, 20), getInt(p, 28), getInt(p, 32)
            );
            Stats.getInstance().setReceivedOps(1);
            Stats.getInstance().setReceivedDocs(reply.getReplyDocs());

            this.lock.lock();
            IReply replyFunc = this.replyFuncs.get(responseTo & 0xff);
            if(replyFunc != null){
                this.replyFuncs.remove(responseTo & 0xff);
            }
            this.lock.unlock();
            if(replyFunc != null && reply.getReplyDocs() == 0){
                replyFunc.reply(null, reply, -1, null);
            } else {
                for(int i=0; i != reply.getReplyDocs(); i++){
                    try {
                        fill(conn, s); //读取此份bson数据的长度信息
                    }catch (IOException e){
                        if(replyFunc != null){
                            replyFunc.reply(new JmgoException(e.getMessage()), null, -1, null);
                        }
                        kill(new JmgoException(e.getMessage()), true);
                        return;
                    }

                    byte[] b = new byte[getInt(s, 0)]; // 用于存储这份bson数据
                    b[0] = s[0];
                    b[1] = s[1];
                    b[2] = s[2];
                    b[3] = s[3];

                    try{
                        fill(conn, b, 4);
                    }catch (IOException e){
                        if(replyFunc != null){
                            replyFunc.reply(new JmgoException(e.getMessage()), null, -1, null);
                        }
                        kill(new JmgoException(e.getMessage()), true);
                        return;
                    }
                    if(replyFunc != null){
                        replyFunc.reply(null, reply, i, b);
                    }
                }
            }
            this.lock.lock();
            if(this.replyFuncs.isEmpty()){
                try{
                    this.conn.setSoTimeout(0);
                }catch (SocketException e){
                    logger.catching(e);
                }
            }else{
                this.updateDeadline();
            }
            this.lock.unlock();
        }
    }

    private boolean dropLogout(Credential cred){
        for(Credential socketCred: this.logout){
            if(socketCred == cred){
                this.logout.remove(cred);
                return true;
            }
        }
        return false;
    }

    private void fill(Socket conn, byte[] b)throws IOException{
        fill(conn, b, 0);
    }

    private void fill(Socket conn, byte[] b, int pos)throws IOException{
        int l = b.length - pos;
        int n = conn.getInputStream().read(b, pos, l);
        while (n != l){
            byte[] nibyte = new byte[l-n];
            int ni = conn.getInputStream().read(b);
            System.arraycopy(nibyte, 0, b, n+pos, ni);
            n += ni;
        }
    }

    private void kill(JmgoException e, boolean abend){
        this.lock.lock();
        if(this.dead!=null){
            logger.debug("Socket {} to {}: killed again");
            this.lock.unlock();
            return;
        }
        logger.info("Socket {} to {}: closing: {} (abend={})", this, this.addr, e.getMessage(), abend);
        this.dead = new SocketDeadException(e.getMessage());
        try{
            this.conn.close();
        }catch (IOException err){
            logger.catching(err);
        }
        Stats.getInstance().setSocketsAlive(-1);
        Map<Integer, IReply> replyFuncs = this.replyFuncs;
        this.replyFuncs = new HashMap<>();
        MongoServer server = this.server;
        this.server = null;
        this.getNonce.signalAll();
        this.lock.unlock();
        for(IReply reply: replyFuncs.values()){
            logger.info("Socket {} to {}: notifying replyFunc of closed socket: {}", this, this.addr, e.getMessage());
            reply.reply(e, null, -1, null);
        }
        if(abend){
            server.abendSocket(this);
        }
    }

    private void updateDeadline(){
        //Socket并没有提供writetimeout和readtimeout，而是合并提交了
        Duration when = Duration.ofSeconds(0);
        if(!this.timeout.isZero()){
            when = Duration.ofSeconds(timeout.getSeconds());
        }
        try {
            this.conn.setSoTimeout((int)when.toMillis());
        }catch (SocketException e){
            logger.catching(e);
        }
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

    private int getInt(byte[] b, int pos){
        int ret = (int)b[pos];
        for(int i=1; i<4; i++){
            ret = ret | ((int)b[pos+i]<<8);
        }
        return ret;
    }

    private long getLong(byte[] b, int pos){
        long ret = (long)b[pos];
        for(int i=1; i<8; i++){
            ret = ret | ((long)b[pos+i]<<8);
        }
        return ret;
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

}
