package com.shahuwang.jmgo.internal.scram;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Created by rickey on 2017/3/27.
 */
public class Client {
    private MessageDigest newHash;
    private String user;
    private String pass;
    private int step;
    private byte[] clientNonce;
    private byte[] serverNonce;
    private byte[] saltedPass;
    private ByteBuffer out;
    private ByteBuffer authMsg;
    private boolean hasErr;

    public Client(MessageDigest newHash, String user, String pass){
        this.newHash = newHash;
        this.user = user;
        this.pass = pass;
        this.out = ByteBuffer.allocate(256);
        this.authMsg = ByteBuffer.allocate(256);
    }

    public byte[] out(){
        return this.out.array();
    }

    public void setNonce(byte[] nonce){
        this.clientNonce = nonce;
    }

//    public boolean Step(byte[] in){
//        this.out.reset();
//        if(this.step > 2 || this.hasErr){
//            return false;
//        }this.step++;
//        if(this.step == 1){
//            this.hasErr = this.step1(in);
//        }else if(this.step == 2){
//            this.hasErr = this.step2(in);
//        }else{
//            this.hasErr = this.step3(in);
//        }
//        return this.step > 2 || this.hasErr;
//    }
//
//    private boolean step1(byte[] in){
//        if(this.clientNonce.length == 0){
//            final int nonceLen = 6;
//
//        }
//        return true
//    }

}
