package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/25.
 */
public class OpQueryFlag {
    public final static int flagTailable = 1 << 2;
    public final static int flagSlaveOk = 1 << 3;
    public final static int flagLogReply = 1 << 3;
    public final static int flagNoCursorTimeout = 1 << 3;
    public final static int flagAwaitData = 1 << 3;
}
