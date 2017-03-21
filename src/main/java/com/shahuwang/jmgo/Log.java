package com.shahuwang.jmgo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Created by rickey on 2017/3/21.
 */
public class Log {
    public static Logger getLogger(String name){
        return LogManager.getLogger(name);
    }
}
