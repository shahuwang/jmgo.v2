package com.shahuwang.jmgo.test;

import com.shahuwang.jmgo.IDialer;
import com.shahuwang.jmgo.MongoServer;
import com.shahuwang.jmgo.ServerAddr;
import com.shahuwang.jmgo.SyncChan;
import com.shahuwang.jmgo.exceptions.JmgoException;
import junit.framework.TestCase;

/**
 * Created by shahuwang on 17-3-31.
 */
public class TestPinger extends TestCase{
    public void testPinger(){
        SyncChan<Boolean> chan = new SyncChan<>();
        ServerAddr addr;
        try {
            addr = new ServerAddr("127.0.0.1:27017");
        }catch (JmgoException e){
            throw new RuntimeException(e.getMessage());
        }
        MongoServer server = new MongoServer(addr, chan, new Dialer());
        try {
            Thread.sleep(1000 * 300);
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }
    }
    class Dialer implements IDialer {};
}
