package com.shahuwang.jmgo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by rickey on 2017/2/24.
 * 用于模仿Go的 chan ， 当 capcity 为 1 的情况
 */
public class SyncChan<E> {
    private BlockingQueue<E> queue = new ArrayBlockingQueue<E>(1);
    Logger logger = LogManager.getLogger(SyncChan.class.getName());
    public void put(E e){
        try{
            this.queue.put(e);
        }catch (InterruptedException exception){
            logger.catching(exception);
            return;
        }
    }

    public void offer(E e){
        // 非阻塞添加，如果队列已满，直接不添加，返回
        this.queue.offer(e);
    }

    public E take() {
        try{
            return this.queue.take();
        }catch (InterruptedException exception){
            logger.catching(exception);
            return null;
        }
    }

    public E poll() {
        try{
            return this.queue.poll(0, TimeUnit.NANOSECONDS);
        }catch (InterruptedException e){
            logger.catching(e);
            return null;
        }
    }

    public E pollTimeout(long timeout, TimeUnit unit) {
        try{
            return this.queue.poll(timeout, unit);
        }catch (InterruptedException e){
            logger.catching(e);
            return null;
        }
    }
}
