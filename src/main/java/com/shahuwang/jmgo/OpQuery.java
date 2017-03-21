package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/21.
 */
public class OpQuery implements IOperator{
    public OpCode getOpCode(){
        return OpCode.OP_QUERY;
    }
}
