package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/4/5.
 */
public class QueryConfig {
    private float prefetch;
    private int limit;
    private OpQuery op;

    public OpQuery getOp() {
        return op;
    }

    public float getPrefetch() {
        return prefetch;
    }

    public int getLimit() {
        return limit;
    }

    public void setPrefetch(float prefetch) {
        this.prefetch = prefetch;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOp(OpQuery op) {
        this.op = op;
    }
}
