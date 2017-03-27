package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/27.
 */
public interface ISaslStepper {
    public void close();
    public Stepper step(byte[] serverData);

    public static class Stepper{
        byte[] clientData;
        boolean done;
    }
}
