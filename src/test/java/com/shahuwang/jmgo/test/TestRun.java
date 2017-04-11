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
    }
    class Dialer implements IDialer {};
}
