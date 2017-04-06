package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/4/6.
 */
public class Safe {
    private int w;
    private String wMode = "";
    private int wTimeout;
    private boolean fsync;
    private boolean j;

    public int getW() {
        return w;
    }

    public String getwMode() {
        return wMode;
    }

    public int getwTimeout() {
        return wTimeout;
    }

    public boolean isFsync() {
        return fsync;
    }

    public boolean isJ() {
        return j;
    }
}
