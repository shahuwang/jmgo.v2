package com.shahuwang.jmgo;

import org.bson.BsonDocument;

/**
 * Created by rickey on 2017/3/21.
 */
public interface IOperator {
    // 所有的对mongo的操作都继承这个operator
    public OpCode getOpCode();
}
