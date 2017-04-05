package com.shahuwang.jmgo;

import org.apache.logging.log4j.Logger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.util.List;

/**
 * Created by rickey on 2017/3/21.
 */
public class OpQuery implements IOperator{
    private String cllection;
    private int skip;
    private int limit;
    private IReply replyFunc;
    private Mode mode;
    private boolean hasOptions;
    private BsonDocument serverTags;
    private int flags;
    private QueryWrapper options;
    private BsonDocument query;
    private BsonDocument selector;

    private Logger logger = Log.getLogger(OpQuery.class.getName());
    public OpQuery(){
        this.mode = Mode.PRIMARY;
        this.flags = OpQueryFlag.flagTailable;
    }
    protected BsonDocument finalQuery(MongoSocket socket){
        if((this.flags & OpQueryFlag.flagSlaveOk) != 0 &&  socket.getServerInfo().isMongos()){
            String modeName = this.mode.getName();
            this.hasOptions = true;
            BsonDocument ops = new BsonDocument("mode", new BsonString(modeName));
            if(this.serverTags.size() > 0){
                ops.append("tags", this.serverTags);
            }
            this.options.setReadPreference(ops);
        }
        if(this.hasOptions){
            if(this.query == null){
                this.options.setQuery(new BsonDocument());
            }else {
                this.options.setQuery(this.query);
            }
            logger.debug("final query is {}\n", this.options);
            return this.options.getDocument();
        }
        return this.query;
    }

    public String getCllection() {
        return cllection;
    }

    public int getFlags() {
        return flags;
    }

    public int getLimit() {
        return limit;
    }

    public BsonDocument getSelector() {
        return selector;
    }

    public IReply getReplyFunc() {
        return replyFunc;
    }

    public int getSkip() {
        return skip;
    }

    public OpQuery setCollection(String collection){
        this.cllection = collection;
        return this;
    }
    public OpQuery setQuery(BsonDocument query){
        this.query = query;
        return this;
    }
    public OpQuery setSkip(int skip){
        this.skip = skip;
        return this;
    }
    public OpQuery setLimit(int limit){
        this.limit = limit;
        return this;
    }
    public OpQuery setSelector(BsonDocument selector){
        this.selector = selector;
        return this;
    }
    public OpQuery setFlags(int flags){
        this.flags = flags;
        return this;
    }
    public OpQuery setReplyFuncs(IReply replyFunc){
        this.replyFunc = replyFunc;
        return this;
    }
    public OpQuery setMode(Mode mode){
        this.mode = mode;
        return this;
    }
    public OpQuery setOptions(QueryWrapper wrapper){
        this.options = wrapper;
        return this;
    }
    public OpQuery setHasOptions(boolean has){
        this.hasOptions = has;
        return this;
    }
    public OpQuery setServerTags(BsonDocument doc){
        this.serverTags = doc;
        return this;
    }

    public BsonDocument getServerTags() {
        return serverTags;
    }

    public OpQuery clone(){
        OpQuery op = new OpQuery();
        op.setCollection(this.cllection)
                .setQuery(this.query).setSkip(this.skip)
                .setLimit(this.limit).setSelector(this.selector)
                .setFlags(this.flags).setReplyFuncs(this.replyFunc)
                .setMode(this.mode).setOptions(this.options)
                .setHasOptions(this.hasOptions).setServerTags(this.serverTags);
        return op;
    }

    public OpCode getOpCode(){
        return OpCode.OP_QUERY;
    }
}
