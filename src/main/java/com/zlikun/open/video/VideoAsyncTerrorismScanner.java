package com.zlikun.open.video;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanRequest;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanResultsRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zlikun.open.consts.AppConstants;
import com.zlikun.open.helper.RequestHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 暴恐涉政视频扫描
 * https://help.aliyun.com/document_detail/62863.html
 * 目前得知扫描过后约一分左右可以获得扫描结果，所以需要控制扫描和查询的时间间隔
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-15 15:46
 */
@Slf4j
public class VideoAsyncTerrorismScanner {

    private static final BlockingQueue<String> TASK_QUEUE = new LinkedBlockingQueue<>();

    private static IAcsClient client;
    private static PrintWriter writer ;
    private static long waitSeconds = 1L;  // 180L => 3分钟

    public static void main(String[] args) throws IOException, ClientException, InterruptedException {

        init();

        // 模拟传入参数
        args = new String [] {
                "E:\\tmp\\video\\video_urls.txt",
        };

        // 视频源文件列表文件
        String source = args[0];


        // 提取有效数据，形成URL列表
        List<String> urls = Files.readAllLines(Paths.get(source))
                .stream()
                .filter(line -> StringUtils.isNotBlank(line) &&
                        !StringUtils.startsWith(line, "#") &&
                        StringUtils.endsWith(line, ".mp4"))
                .map(line -> {
                    if (!StringUtils.startsWithIgnoreCase(line, "http://")) {
                        line = "http://alivideo.zhihuishu.com" + (line.startsWith("/") ? "" : "/") + line;
                    } else if (StringUtils.contains(line, "http://video.zhihuishu.com")) {
                        line = StringUtils.replaceIgnoreCase(line, "http://video.zhihuishu.com", "http://alivideo.zhihuishu.com");
                    }
                    return line;
                })
                .collect(Collectors.toList());

        // 启动一个守护线程读取视频文件列表
        start();

        // 上面已经过滤过，所以实际并不需要
        urls.stream().forEach(VideoAsyncTerrorismScanner::check);

        // 等待队列空
        while (!TASK_QUEUE.isEmpty()) {
            TimeUnit.SECONDS.sleep(5L);
        }

        log.info("==== COMPLETE ====");

    }

    private static final void init() throws ClientException, IOException {
        IClientProfile profile = DefaultProfile.getProfile(AppConstants.regionId, AppConstants.accessKey, AppConstants.accessSecret);
        DefaultProfile.addEndpoint(AppConstants.endpoint, AppConstants.regionId, "Green", AppConstants.domain);
        client = new DefaultAcsClient(profile);
        writer = new PrintWriter(new FileWriter("E:\\tmp\\video\\video_terrorism_results.log"));
    }

    /**
     * 起动守护线程，用于获取扫描的结果
     */
    private static final void start() {
        Thread daemon = new Thread(getResultRunnable());
        daemon.setName("consumer");
        daemon.setDaemon(true);
        daemon.start();
    }

    private static final Runnable getResultRunnable() {
        return () -> {
            // 任务休眠3分钟，再执行查询结果任务(扫描任务需要异步执行一定时间)
            // 该方案假定执行扫描任务和执行查询任务耗时相差不大
            log.info("起动守护线程 ...");
            try {
                TimeUnit.SECONDS.sleep(waitSeconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("开始查询结果 ...");
            // 循环出队，查询扫描结果
            while (true) {
                try {
                    String taskId = TASK_QUEUE.take();
                    if (taskId == null) continue;
                    result(taskId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 获取结果
     * @param taskId
     */
    private static final void result(String taskId) {

        if (taskId == null) return;
//        log.info("taskId = {}", taskId);

        // 每隔200毫秒执行一个请求
        try {
            TimeUnit.MILLISECONDS.sleep(200L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        VideoAsyncScanResultsRequest request = new VideoAsyncScanResultsRequest();

        JSONArray data = new JSONArray();
        data.add(taskId);

        try {
            request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RequestHelper.execute(client, request, jsonObject -> {
//            // 仅供调试使用
//            System.out.println(JSON.toJSONString(jsonObject, true));

            String json = jsonObject.toJSONString();
            // 判断是否仍在扫描中，如果在，则递归重复扫
            if (StringUtils.contains(json, "\"code\":280") && StringUtils.contains(json, "PROCESSING")) {
                // 递归时，休眠3秒
                try {
                    TimeUnit.SECONDS.sleep(3L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                result(taskId);
                return;
            }

            // 将结果输出到文件
            print(json);
        });

    }

    /**
     * 检查视频
     * @param url
     */
    private static final void check(String url) {
        // 每隔200毫秒，最多执行一个任务
        try {
            TimeUnit.MILLISECONDS.sleep(200L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 每个任务最多重试2次（共3次）
        for (int i = 0; i < 3; i++) {
            try {
                scan(url);
                return;
            } catch (Throwable t) {
                log.warn("扫描[{}]失败{}次，消息：{}", url, i + 1, t.getMessage());
                // 发生重试时，尝试扩大休眠时间
                try {
                    TimeUnit.SECONDS.sleep(i + 1L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // 重试三次，仍执行失败
        log.error("经过重试，扫描[{}]仍失败!", url);
    }

    /**
     * 实际扫描逻辑
     * https://help.aliyun.com/document_detail/62863.html
     * @param url
     * @throws Exception
     */
    private static final void scan(String url) throws Exception {
        // 执行扫描，并将扫描回执加入队列，由队列监听者查询结果

        log.info("scan url -> {}", url);

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
        data.put("scenes", Arrays.asList("terrorism"));
        data.put("tasks", tasks);

        try {
            request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 执行涉政请求
        /* ---------------------------------------------------------------------------------------------------------------------------------
        {
            "msg":"OK",
            "code":200,
            "data":[
                {
                    "msg":"OK",
                    "code":200,
                    "dataId":"7dd8549a390d455f8f291d1fd0f45413",
                    "taskId":"vi4fHwlKQ$4Lq6MHNfX@a9DL-1oyyXD",
                    "url":"http://alivideo.zhihuishu.com/testzhs/createcourse/COURSE/201608/70f7e17d1268497eafdeeb359c44127f_500.mp4"
                }
            ],
            "requestId":"2CBE5310-ABAB-4D33-BBA8-093F53CF91AE"
        }
        --------------------------------------------------------------------------------------------------------------------------------- */
        RequestHelper.execute(client, request, jsonObject -> {
//            System.out.println(JSON.toJSONString(jsonObject, true));
            // 提取taskId字段
            JSONArray array = jsonObject.getJSONArray("data");
            if (array == null || array.isEmpty()) return;
            JSONObject dataObject = array.getJSONObject(0);
            // 记录任务信息，后续将根据任务，获取扫描结果
            // 提交 taskId 到队列中
            try {
                TASK_QUEUE.put(dataObject.getString("taskId"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

    }

    /**
     * 将内容打印到文件中
     * @param content
     */
    private static final void print(String content) {
        writer.println(content);
        writer.flush();
    }

}
