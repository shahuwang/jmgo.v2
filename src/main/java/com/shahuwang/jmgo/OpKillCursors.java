package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/26.
 */
public class OpKillCursors implements IOperator{
    private long[] cursorIds;

    public long[] getCursorIds() {
        return cursorIds;
    }

    public OpKillCursors setCursorIds(long[] cursorIds) {
        this.cursorIds = cursorIds;
        return this;
    }

    public OpCode getOpCode(){
        return OpCode.OP_KILL_CURSORS;
    }
}
