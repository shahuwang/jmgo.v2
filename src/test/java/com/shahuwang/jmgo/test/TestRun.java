package com.shahuwang.jmgo.test;

import com.shahuwang.jmgo.*;
import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.NotFoundError;
import junit.framework.TestCase;
import org.bson.BsonDocument;

import java.time.Duration;

/**
 * Created by rickey on 2017/4/7.
 */
public class TestRun extends TestCase{
    public void testRun(){
        ServerAddr addr = new ServerAddr("localhost", 27017);
        ServerAddr[] unusedSedds = {addr};
        MongoCluster cluster = new MongoCluster(unusedSedds, true, true, new Dialer(), "");
        MongoSession session = new MongoSession(Mode.PRIMARY, cluster, Duration.ofSeconds(10));
        try {
            BsonDocument doc = session.run("ping");
            System.out.println(doc.toJson());
        }catch (JmgoException | NotFoundError e){
            System.out.println("=====================");
        }
        byte[] b = {-87, 0, 0, 0};
        System.out.println(getInt(b, 0));
    }
    private int getInt(byte[] b, int pos){
        int ret = b[pos] & 0xff;
        for(int i=1; i<4; i++){
            ret = ret | ((b[pos+i]<<(8 * i))&0xff);
        }
        return ret;
    }
    class Dialer implements IDialer {};
}
