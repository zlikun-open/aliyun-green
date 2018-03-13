package com.zlikun.open;

import com.alibaba.fastjson.JSON;
import com.zlikun.open.consts.AppConstants;
import com.zlikun.open.helper.HmacSHA1Helper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 视频鉴黄API测试
 * https://help.aliyun.com/document_detail/57420.html
 * https://help.aliyun.com/knowledge_list/50168.html
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 09:05
 */
@Slf4j
public class PornVideoTest {

    private OkHttpClient client = new OkHttpClient.Builder()
            .build();

    @Test
    public void test() throws Exception {

        final String apiUrl = "https://green.cn-hangzhou.aliyuncs.com/green/video/asyncscan";

        // 构建鉴黄请求参数
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        // 调用者通常保证一次请求中，所有的dataId不重复
        data.put("dataId", "v0000001");
        // 视频地址，和frames不能同时为空，也不能同时有值，传入本字段将按照传url的方式计费
        data.put("url", "http://oss-cn-hangzhou-internal.aliyuncs.com/zhs/createcourse/course_second/201803/4518adbea7cc4c1eb5c5c698dfebcf0c_512.mp4");
        // 视频截帧间隔，单位秒；取值范围为[1, 60]; 默认为1秒
        data.put("interval", 5);
        // 截帧最多张数；取值范围为[5, 200]; 默认为200张。当用OSS地址（以”oss://“开头）时，最大允许张数为20000张
        data.put("maxFrames", 100);
        list.add(data);

        Map<String, Object> params = new HashMap<>();
//        // 业务类型，调用方从云盾内容安全申请所得。每个bizType对应不同的算法/模型。根据配置，后端可根据该字段对请求做不同处理。属于高级用法
//        params.put("bizType", "");
        // 字符串数组，场景定义参考1.1小节；最终执行的算法为该列表里的算法和该用户在后端配置允许的算法的交集
        params.put("scenes", "porn");
//        // 异步检测结果回调通知用户url；支持http/https。当该字段为空时，用户必须定时检索检测结果
//        params.put("callback", "");
//        // 该值会用户回调通知请求中签名；当含有callback时，该字段为必须
//        params.put("seed", "");
        params.put("tasks", list);
        final String json = JSON.toJSONString(params);
        log.info("请求参数：{}", json);

        String md5 = Base64.getEncoder().encodeToString(DigestUtils.md5(json));

        Request.Builder builder = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                ;

        // 下面是公共请求头
        // https://help.aliyun.com/document_detail/53413.html
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateString = format.format(new Date());
        builder
                // 接受的返回类型，目前只支持JSON：application/json
                .addHeader("Accept", "application/json")
                // 当前请求body的数据类型，目前只支持JSON：application/json
                .addHeader("Content-Type", "application/json")
                // 根据请求body计算所得，算法为先对body做md5，再做base64编码所得
                .addHeader("Content-MD5", md5)
                // GMT日期格式，例如：Tue, 17 Jan 2017 10:16:36 GMT
                .addHeader("Date", dateString);

        // x-acs- 消息头
        Map<String, String> acsHeaders = new HashMap<>();
        // 内容安全接口版本，当前版本为：2017-01-12
        acsHeaders.put("x-acs-version", "2017-01-12");
        // 随机字符串，用来避免回放攻击
        acsHeaders.put("x-acs-signature-nonce", UUID.randomUUID().toString());
        // 签名版本，目前取值：1.0
        acsHeaders.put("x-acs-signature-version", "1.0");
        // 签名方法，目前只支持: HMAC-SHA1
        acsHeaders.put("x-acs-signature-method", "HMAC-SHA1");

        // 加入`x-acs-`消息头
        acsHeaders.forEach((k, v) -> {
            builder.addHeader(k, v);
        });

        // 针对`x-acs-`消息头计算签名
        // 1) 序列化请求头
        String acsText = acsHeaders.entrySet().stream()
                .sorted((m, n) -> m.getKey().compareTo(n.getKey()))
                .map(entry -> entry.getKey() + ":" + entry.getValue() + "\n")
                .reduce((m, n) -> m + n)
                .get();
        log.info("acsText => \n{}", acsText);

        // 2) 序列化uri和query参数
        // 3) 构建完整的待签名字符串
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("POST").append("\n");
        stringBuilder.append("application/json").append("\n");
        stringBuilder.append(md5).append("\n");
        stringBuilder.append("application/json").append("\n");
        stringBuilder.append(dateString).append("\n");
        stringBuilder.append(acsText);
        stringBuilder.append("/green/video/asyncscan");

        builder
                // 认证方式，取值格式为："acs" + " " + AccessKeyId + ":" + signature。其中AccessKeyId从阿里云控制台申请所得，而signature为请求签名。签名算法参见后面文档1.3说明。
                // https://help.aliyun.com/document_detail/53415.html
                .addHeader("Authorization", String.format("acs %s:%s", AppConstants.accessKey,
                        HmacSHA1Helper.encrypt(stringBuilder.toString(), AppConstants.accessSecret)));

        Request request = builder.build();

        Response response = client.newCall(request).execute();
        log.info("code = {}, message = {}", response.code(), response.message());

        log.info("response content => \n{}", response.body().string());
    }

    @Test
    public void other() {
        // Tue, 13 Mar 2018 15:23:59 GMT
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)));
        // Tue, 13 Mar 2018 07:23:59 GMT
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        System.out.println(format.format(new Date()));
    }

}
