package com.zlikun.open.video;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.AcsRequest;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zlikun.open.consts.AppConstants;
import com.zlikun.open.helper.RequestHelper;
import com.zlikun.open.helper.ResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 视频异步扫描入口
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 16:45
 */
@Slf4j
public class VideoAsyncScanMain {

    private static VideoAsyncScanMain INSTANCE;

    static {
        try {
            INSTANCE = new VideoAsyncScanMain();
        } catch (ClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            args = new String [] {
                    "src/main/resources/data/video_url_exclude.txt",
                    "src/main/resources/data/video_url_list.txt"
            };
        }

        // 获取排除列表集合 ( 已执行成功的任务 )
        Set<String> sets = Files.readAllLines(Paths.get(args[0]))
                .stream()
                .map(line -> StringUtils.substringAfterLast(line, ","))
                .collect(Collectors.toSet());

        // 遍历链接数据文件
        final AtomicInteger counter = new AtomicInteger();
        Files.readAllLines(Paths.get(args[1]))
                .stream()
                .map(url -> StringUtils.trim(url))
                .filter(url -> url != null && !sets.contains(url))
                .forEach(url -> {
                    log.info("Scan {} -> {}", counter.incrementAndGet(), url);
                    scan(url);
                    // 程序休眠一秒钟，保证一秒只提交一个任务
                    try {
                        TimeUnit.SECONDS.sleep(1L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

    }

    /**
     * 执行任务
     * @param url
     */
    private static final void scan(String url) {
        for (int i = 0; i < 3; i++) {
            try {
                scan(url, i);
                return;
            } catch (Exception e) {
               log.error("执行扫描任务出错!", e);
            }
        }
        log.error("任务[{}]重试指定次数后，仍执行失败!", url);
    }

    /**
     * 执行任务实际逻辑，记录重试逻辑
     * @param url
     * @param index
     */
    private static final void scan(String url, int index) throws Exception {
        if (index > 0) {
            log.warn("任务[{}]重试第{}次~", url, index);
            // 重试时休眠重试次数秒，防止请求过快造成失败
            try {
                TimeUnit.SECONDS.sleep(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 实际执行逻辑
        INSTANCE.execute(url);
    }

    protected IAcsClient client;
    private PrintWriter writer ;

    public VideoAsyncScanMain() throws ClientException, IOException {
        IClientProfile profile = DefaultProfile.getProfile(AppConstants.regionId, AppConstants.accessKey, AppConstants.accessSecret);
        DefaultProfile.addEndpoint(AppConstants.endpoint, AppConstants.regionId, "Green", AppConstants.domain);
        client = new DefaultAcsClient(profile);
        writer = new PrintWriter(new FileWriter("/tasks.log"));
    }

    /**
     * 执行视频扫描任务
     * @param url
     */
    private final void execute(String url) {

        // https://help.aliyun.com/document_detail/57420.html
        VideoAsyncScanRequest request = new VideoAsyncScanRequest();
        request.setMethod(MethodType.POST);

        List<Map<String, Object>> tasks = new ArrayList<>();
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("dataId", StringUtils.replaceAll(UUID.randomUUID().toString(), "-", ""));
        task.put("url", url);
        // 每3秒扫描一次
        task.put("interval", 5);
        // 截帧最多张数
        task.put("maxFrames", 60);

        tasks.add(task);

        JSONObject data = new JSONObject();
        data.put("scenes", Arrays.asList("porn"));
        data.put("tasks", tasks);

        try {
            request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 执行鉴黄请求
        RequestHelper.execute(client, request, jsonObject -> {
            // 提取taskId字段
            JSONArray array = jsonObject.getJSONArray("data");
            if (array == null || array.isEmpty()) return;
            JSONObject dataObject = array.getJSONObject(0);
            // 记录任务信息，后续将根据任务，获取扫描结果
            // 打印任务ID等信息
            // dataId,taskId,url
            print(dataObject.getString("dataId") +
                    "," + dataObject.getString("taskId") +
                    "," + dataObject.getString("url"));
        });

    }

    /**
     * 将内容打印到文件中
     * @param content
     */
    private void print(String content) {
        writer.println(content);
        writer.flush();
    }

}
