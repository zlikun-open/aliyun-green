package com.zlikun.open;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.AcsRequest;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.zlikun.open.consts.AppConstants;
import org.junit.Before;

import java.io.UnsupportedEncodingException;

/**
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 15:44
 */
public abstract class VideoAsyncScanBase {

    protected String regionId = "cn-hangzhou";
    protected String endpoint = regionId;
    protected String domain = "green.cn-hangzhou.aliyuncs.com";

    protected IAcsClient client;

    @Before
    public void init() throws ClientException {
        IClientProfile profile = DefaultProfile.getProfile(regionId, AppConstants.accessKey, AppConstants.accessSecret);
        DefaultProfile.addEndpoint(endpoint, regionId, "Green", domain);
        client = new DefaultAcsClient(profile);
    }

    /**
     *
     */
    protected void execute(AcsRequest request) throws UnsupportedEncodingException {

        // 指定API返回格式
        request.setAcceptFormat(FormatType.JSON);
        request.setContentType(FormatType.JSON);

        // 请务必设置超时时间
        request.setConnectTimeout(3000);
        request.setReadTimeout(6000);

        try {
            HttpResponse httpResponse = client.doAction(request);

            if (httpResponse.isSuccess()) {
                JSONObject jo = JSON.parseObject(new String(httpResponse.getContent(), "UTF-8"));
                System.out.println(JSON.toJSONString(jo, true));
            } else {
                System.out.println("response not success. status:" + httpResponse.getStatus());
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

}
