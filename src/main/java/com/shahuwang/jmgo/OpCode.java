package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/21.
 */
public enum OpCode {
    OP_REPLY(1), OP_MSG(1000), OP_UPDATE(2001),
    OP_INSERT(2002), RESERVED(2003), OP_QUERY(2004),
    OP_GET_MORE(2005), OP_DELETE(2006), OP_KILL_CURSORS(2007),
    OP_COMMAND(2010), OP_COMMANDREPLY(2011);

    private int code;
    private OpCode(int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
