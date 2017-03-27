package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;

/**
 * Created by rickey on 2017/2/23.
 */
public interface IDialer {
    Logger logger = LogManager.getLogger(IDialer.class.getName());

    default Socket dial(ServerAddr addr, Duration timeout) throws JmgoException {
        InetSocketAddress tcpaddr = addr.getTcpaddr();
        Socket socket;
        try{
            socket = new Socket(tcpaddr.getAddress(), tcpaddr.getPort());
        }catch (IOException e){
            logger.info("Dial {}:{} get an error", tcpaddr.getAddress(), tcpaddr.getPort());
            logger.catching(e);
            throw new JmgoException(e.getMessage());
        }
        try{
            socket.setSoTimeout((int)(timeout.getSeconds() * 1000));
            socket.setKeepAlive(true);
        }catch (SocketException e){
           logger.info("Dial get an error with set timeout or set keep alive {}", e.getMessage());
           logger.catching(e);
           throw new JmgoException(e.getMessage());
        }
        return socket;
    }
}
