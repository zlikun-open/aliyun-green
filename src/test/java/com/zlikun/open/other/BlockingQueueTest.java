package com.zlikun.open.other;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;

/**
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-15 17:30
 */
public class BlockingQueueTest {

    @Test
    public void test() throws InterruptedException {

        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

        queue.put(12);
        queue.put(18);
        queue.put(27);

        assertEquals(Integer.valueOf(12), queue.take());
        assertEquals(Integer.valueOf(18), queue.take());
        assertEquals(Integer.valueOf(27), queue.take());

    }

}
