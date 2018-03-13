package com.zlikun.open;

import com.alibaba.fastjson.JSONArray;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanResultsRequest;
import com.aliyuncs.http.FormatType;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * 查询扫描结果测试
 *
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 15:42
 */
public class VideoAsyncScanResultsTest extends VideoAsyncScanBase {

    private String taskId = "vi4nwZFEfukSF6d826ayxGSO-1oxV@D";

    @Test
    public void test() throws UnsupportedEncodingException {

        VideoAsyncScanResultsRequest request = new VideoAsyncScanResultsRequest();

        JSONArray data = new JSONArray();
        data.add(taskId);

        request.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);

        // 执行请求
        // https://help.aliyun.com/document_detail/57420.html
        execute(request);

    }

}
