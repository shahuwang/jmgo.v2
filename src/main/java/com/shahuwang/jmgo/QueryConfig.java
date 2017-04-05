package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/4/5.
 */
public class QueryConfig {
    private long prefetch;
    private int limit;
    private OpQuery op;

    public OpQuery getOp() {
        return op;
    }

    public long getPrefetch() {
        return prefetch;
    }

    public int getLimit() {
        return limit;
    }

    public void setPrefetch(long prefetch) {
        this.prefetch = prefetch;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOp(OpQuery op) {
        this.op = op;
    }
}
