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
import com.aliyuncs.green.model.v20170112.VideoAsyncScanResultsRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zlikun.open.consts.AppConstants;
import com.zlikun.open.helper.ResponseHandler;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * 视频异步扫描入口
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 16:45
 */
public class VideoAsyncScanMain {

    public static void main(String[] args) throws ClientException {

        final String prefix = "http://alivideo.zhihuishu.com/";

        // 视频列表
        List<String> list = new ArrayList<>();
        list.add("zhs/ablecommons/demo/201803/d2dd5a0a980e476994f9616b801d81c1_512.mp4");

        // 遍历视频，并执行扫描任务
        final VideoAsyncScanMain main = new VideoAsyncScanMain();
        list.stream()
                .distinct()
                .map(url -> prefix + url)
                .forEach(main::execute);

    }

    protected String regionId = "cn-hangzhou";
    protected String endpoint = regionId;
    protected String domain = "green.cn-hangzhou.aliyuncs.com";

    protected IAcsClient client;

    public VideoAsyncScanMain() throws ClientException {
        IClientProfile profile = DefaultProfile.getProfile(regionId, AppConstants.accessKey, AppConstants.accessSecret);
        DefaultProfile.addEndpoint(endpoint, regionId, "Green", domain);
        client = new DefaultAcsClient(profile);
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
        task.put("dataId", UUID.randomUUID().toString());
        task.put("url", url);
        // 每3秒扫描一次
        task.put("interval", 3);
        // 截帧最多张数
        task.put("maxFrames", 100);

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
        execute(request, jsonObject -> {
            // 提取taskId字段
            JSONObject dataObject = jsonObject.getJSONArray("data").getJSONObject(0);
            // 记录任务信息，后续将根据任务，获取扫描结果
            // dataId,taskId,url
            System.out.println(dataObject.getString("dataId") +
                    "," + dataObject.getString("taskId") +
                    "," + dataObject.getString("url"));
//            // 执行结果查询请求
//            scanResults(dataObject.getString("taskId"));
        });

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
//                    System.out.println(JSON.toJSONString(jsonObject, true));
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

    /**
     * 获取扫描结果
     * @param taskId
     */
    void scanResults(String taskId) {
        if (taskId == null) return;

        VideoAsyncScanResultsRequest request = new VideoAsyncScanResultsRequest();

        JSONArray data = new JSONArray();
        data.add(taskId);

        try {
            request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 执行请求
        // https://help.aliyun.com/document_detail/57420.html
        execute(request, jsonObject -> {
            System.out.println(JSON.toJSONString(jsonObject, true));
        });

    }

}
