package com.zlikun.open.video;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.AcsRequest;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanResultsRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zlikun.open.consts.AppConstants;
import com.zlikun.open.helper.ResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 视频异步扫描结果查询
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 16:45
 */
@Slf4j
public class VideoAsyncResultsMain {

    private static VideoAsyncResultsMain INSTANCE;

    static {
        try {
            INSTANCE = new VideoAsyncResultsMain();
        } catch (ClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            args = new String [] {
                    "src/main/resources/data/video_url_tasks_exclude.txt",
                    "src/main/resources/data/video_url_tasks.txt"
            };
        }

        // 获取排除列表集合 ( 已执行成功的任务 )
        Set<String> sets = Files.readAllLines(Paths.get(args[0]))
                .stream()
                // 三部分信息分别为：dataId,taskId,url，所以取第二部分
                .map(line -> StringUtils.substringBetween(line, ",", ","))
                .collect(Collectors.toSet());

        // 遍历链接数据文件
        final AtomicInteger counter = new AtomicInteger();
        Files.readAllLines(Paths.get(args[1]))
                .stream()
                .map(line -> StringUtils.substringBetween(line, ",", ","))
                .filter(taskId -> taskId != null && !sets.contains(taskId))
                .forEach(taskId -> {
                    log.info("Result {} -> {}", counter.incrementAndGet(), taskId);
                    result(taskId);
                    // 程序休眠500毫秒，控制任务提交速度
                    try {
                        TimeUnit.MILLISECONDS.sleep(500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

    }

    /**
     * 执行任务
     * @param taskId
     */
    private static final void result(String taskId) {
        for (int i = 0; i < 3; i++) {
            try {
                result(taskId, i);
                return;
            } catch (Exception e) {
               log.error("执行扫描任务出错!", e);
            }
        }
        log.error("任务[{}]重试指定次数后，仍执行失败!", taskId);
    }

    /**
     * 执行任务实际逻辑，记录重试逻辑
     * @param taskId
     * @param index
     */
    private static final void result(String taskId, int index) throws Exception {
        if (index > 0) {
            log.warn("任务[{}]重试第{}次~", taskId, index);
            // 重试时休眠重试次数秒，防止请求过快造成失败
            try {
                TimeUnit.SECONDS.sleep(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 实际执行逻辑
        INSTANCE.execute(taskId);
    }

    protected String regionId = "cn-shanghai";
    protected String endpoint = regionId;
    protected String domain = "green.cn-shanghai.aliyuncs.com";

    protected IAcsClient client;
    private PrintWriter writer ;

    public VideoAsyncResultsMain() throws ClientException, IOException {
        IClientProfile profile = DefaultProfile.getProfile(regionId, AppConstants.accessKey, AppConstants.accessSecret);
        DefaultProfile.addEndpoint(endpoint, regionId, "Green", domain);
        client = new DefaultAcsClient(profile);
        writer = new PrintWriter(new FileWriter("/tasks.log"));
    }

    /**
     * 执行视频扫描结果查询
     * @param taskId
     */
    private final void execute(String taskId) {

        if (taskId == null) return;

        VideoAsyncScanResultsRequest request = new VideoAsyncScanResultsRequest();

        JSONArray data = new JSONArray();
        data.add(taskId);

        try {
            request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 执行鉴黄请求
        /* ---------------------------------------------------------------------------------------------------------
        {
            "msg":"OK",
            "code":200,
            "data":[
                {
                    "msg":"OK",
                    "code":200,
                    "dataId":"eceebcc16171481dbedf1439f23ce52d",
                    "results":[
                        {
                            "rate":99.9,
                            "suggestion":"pass",
                            "label":"normal",
                            "scene":"porn"
                        }
                    ],
                    "taskId":"vi3J1kL1bKAfM57XbZceexEh-1oyukv",
                    "url":"http://alivideo.zhihuishu.com/zhs/createcourse/COURSE/201708/afb9cad8908842068c28dbbf94b133c8_500.mp4"
                }
            ],
            "requestId":"43C3C1D0-9C19-4441-BD57-0E5033A07D0C"
        }
        --------------------------------------------------------------------------------------------------------- */
        execute(request, jsonObject -> {
//            // 仅供调试使用
//            System.out.println(JSON.toJSONString(jsonObject, true));
            print(jsonObject.toJSONString());
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

    private void execute(AcsRequest request, ResponseHandler<JSONObject> handler) {

        // 指定API返回格式
        request.setAcceptFormat(FormatType.JSON);
        request.setContentType(FormatType.JSON);

        // 请务必设置超时时间
        request.setConnectTimeout(3000);
        request.setReadTimeout(6000);

        try {
            HttpResponse httpResponse = client.doAction(request);

            if (httpResponse.isSuccess()) {
                JSONObject jsonObject = JSON.parseObject(new String(httpResponse.getContent(), "UTF-8"));
                if (jsonObject != null) {
                    handler.handle(jsonObject);
                }
            } else {
                System.out.println("response not success. status:" + httpResponse.getStatus());
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
