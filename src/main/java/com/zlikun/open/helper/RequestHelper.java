package com.zlikun.open.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.AcsRequest;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;

/**
 * 请求工具类
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-15 16:07
 */
@Slf4j
public class RequestHelper {

    /**
     * 执行请求
     * @param client
     * @param request
     * @param handler
     */
    public static final void execute(IAcsClient client, AcsRequest request, ResponseHandler<JSONObject> handler) {

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
                log.warn("Response not success, status is {} .", httpResponse.getStatus());
            }
        } catch (ServerException e) {
            log.error("服务端异常!", e);
        } catch (ClientException e) {
            log.error("客户端异常!", e);
        } catch (UnsupportedEncodingException e) {
            log.error("不支持的字符集!", e);
        }
    }

}
