package com.zlikun.open;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.MethodType;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * 使用官方SDK扫描视频 ( 异步 )
 *
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 15:32
 */
public class VideoAsyncScanRequestTest extends VideoAsyncScanBase {

    private String prefix = "http://alivideo.zhihuishu.com/";
    private String videoUrl = prefix + "zhs/ablecommons/demo/201803/d2dd5a0a980e476994f9616b801d81c1_512.mp4";

    @Test
    public void test() throws UnsupportedEncodingException {

        // https://help.aliyun.com/document_detail/57420.html
        VideoAsyncScanRequest request = new VideoAsyncScanRequest();
        request.setMethod(MethodType.POST);

        List<Map<String, Object>> tasks = new ArrayList<>();
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("dataId", UUID.randomUUID().toString());
        task.put("interval", 1);
        task.put("length", 180);
        task.put("url", videoUrl);

        tasks.add(task);

        JSONObject data = new JSONObject();
        data.put("scenes", Arrays.asList("porn"));
        data.put("tasks", tasks);

        request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);

        // 执行请求
        execute(request);

    }

}
