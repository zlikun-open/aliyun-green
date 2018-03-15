package com.zlikun.open.other;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-15 13:16
 */
public class OtherTest {

    /**
     * 扫描结果中的过期任务数据
     * @throws IOException
     */
    @Test
    public void expire_tasks() throws IOException {

        Files.readAllLines(Paths.get("src/main/resources/data/video_url_tasks_result.txt"))
                .stream()
                .filter(line -> line != null && line.contains("\"msg\":\"[taskId] expired\",\"code\":594"))
                .map(line -> {
                    return StringUtils.substringBetween(line, "\"taskId\":\"", "\"}],\"requestId\"");
                })
                .forEach(System.out::println);

    }

    @Test
    public void test() throws IOException {

        final AtomicInteger counter = new AtomicInteger();
        Files.readAllLines(Paths.get("src/main/resources/data/video_url_tasks_result.txt"))
                .stream()
                .filter(line -> line != null && !line.contains("\"msg\":\"[taskId] expired\",\"code\":594"))
                .filter(line -> !line.contains("{\"rate\":99.9,\"suggestion\":\"pass\",\"label\":\"normal\",\"scene\":\"porn\"}"))
                .filter(line -> !line.contains("[content-length] should not be >"))
                .filter(line -> !line.contains(".mp3\"}]"))
                .filter(line -> !line.contains("\"msg\":\"404 Not Found\""))
                .filter(line -> !line.contains("\"msg\":\"403 Forbidden\""))
                .filter(line -> !line.contains("snapshot:InvalidParameter.ResourceContentBad The resource operated InputFile is bad"))
                .forEach(line -> {
                    System.out.println(counter.incrementAndGet() + " -> " + line);
                });

    }

}
