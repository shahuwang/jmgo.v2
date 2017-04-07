package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.NoReachableServerException;
import com.shahuwang.jmgo.exceptions.NotFoundError;
import com.shahuwang.jmgo.exceptions.SessionClosedException;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.io.BsonInput;
import org.bson.io.ByteBufferBsonInput;

import java.nio.ByteBuffer;


/**
 * Created by rickey on 2017/4/5.
 */
public class Database {
    private MongoSession session;
    private String name;
    private static Codec<BsonDocument> DocumentCodec = new BsonDocumentCodec();

    public Database(MongoSession session, String name){
        this.session = session;
        this.name = name;
    }

    public MongoSession getSession() {
        return session;
    }

    public String getName() {
        return name;
    }

    public BsonDocument run(BsonDocument cmd)throws SessionClosedException, NoReachableServerException, JmgoException, NotFoundError{
        MongoSocket socket = this.session.acquireSocket(true);
        BsonDocument ret = this.run(socket, cmd);
        socket.release();
        return ret;
    }

    public BsonDocument run(String cmd)throws SessionClosedException, NoReachableServerException, JmgoException, NotFoundError{
        MongoSocket socket = this.session.acquireSocket(true);
        BsonDocument ret = this.run(socket, cmd);
        socket.release();
        return ret;
    }

    private BsonDocument run(MongoSocket socket, BsonDocument cmd)throws JmgoException, NotFoundError{
       MongoSession session = this.session;
       session.getLock().readLock().lock();
       OpQuery op = session.getQueryConfig().getOp().clone();
       session.getLock().readLock().unlock();
       op.setQuery(cmd);
       op.setCollection(this.name + ".$cmd");
       session.prepareQuery(op);
       op.setLimit(-1);
       byte[] data = socket.simpleQuery(op);
       if(data == null){
           throw new NotFoundError();
       }
       ByteBuf buf = new ByteBufNIO(ByteBuffer.allocate(data.length));
       BsonInput binput = new ByteBufferBsonInput(buf);
       BsonBinaryReader reader = new BsonBinaryReader(binput);
       return DocumentCodec.decode(reader, DecoderContext.builder().build());
    }

    private BsonDocument run(MongoSocket socket, String cmd)throws JmgoException, NotFoundError{
        BsonDocument query = new BsonDocument(cmd, new BsonInt32(1));
        return run(socket, query);
    }
}
