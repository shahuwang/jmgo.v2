package com.shahuwang.jmgo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by rickey on 2017/2/28.
 */
public class BuildConfig {
    private static BuildConfig ourInstance = new BuildConfig();
    private Properties prop = new Properties();

    public static BuildConfig getInstance() {
        return ourInstance;
    }

    Logger logger = LogManager.getLogger(BuildConfig.class.getName());
    private BuildConfig() {
        try{
            String filename = getClass().getResource("/build.properties").getFile();
            InputStream in = new FileInputStream(filename);
            prop.load(in);
        }catch (FileNotFoundException e){
            logger.error(e);

        }catch (IOException e){
            logger.error(e);
        }
    }

    public Properties getProp() {
        return prop;
    }

    public boolean getRacedetector(){
        String race = this.prop.getProperty("racedetector");
        return Boolean.parseBoolean(race);
    }
}
