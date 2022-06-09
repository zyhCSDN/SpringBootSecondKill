package com.debug.kill.server.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class COSClientInit {

    //手动创建线程池

    private static ArrayBlockingQueue queue = new ArrayBlockingQueue(8, true);

    private static ThreadPoolExecutor.CallerRunsPolicy policy = new ThreadPoolExecutor.CallerRunsPolicy();

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 8, 10, TimeUnit.SECONDS, queue, policy);

    static String bucketName = "zhaoyongheng" + "-" + "1308629741"; //桶的名称

    /**
     * 高级api
     */
    public static TransferManager transferManager = new TransferManager(COSClientInit.CreateCOSClient(), executor);

    public static COSClient CreateCOSClient() {
        // 1 初始化用户身份信息（secretId, secretKey）。
        // SECRETID和SECRETKEY请登录访问管理控制台 https://console.cloud.tencent.com/cam/capi 进行查看和管理
        //SecretId 是用于标识 API 调用者的身份
        String secretId = "AKID5zd07yZtwiDQO5WgZCZfU2MSIsBDyUkJ";
        //SecretKey是用于加密签名字符串和服务器端验证签名字符串的密钥
        String secretKey = "LagY9WeVAbokPnulMhlZ6ML4jGXZOzJO";
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region("ap-nanjing");
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }

    /**
     * 获取存储桶列表
     *
     * @return
     */
    public static List<Bucket> getBucket() {
        return COSClientInit.CreateCOSClient().listBuckets();
    }

    /**
     * 上传文件到腾讯云对象存储
     *
     * @param file       文件对象
     * @param bucketName 存储桶名称
     * @param key        对象键（Key）是对象在存储桶中的唯一标识。例如，在对象的访问域名 examplebucket-1250000000.cos.ap-guangzhou.myqcloud.com/images/picture.jpg 中，对象键为 images/picture.jpg
     * @return
     */
    public static PutObjectResult uploadFile(File file, String bucketName, String key) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
        return COSClientInit.CreateCOSClient().putObject(putObjectRequest);
    }

    /**
     * 上传文件到腾讯云对象存储
     *
     * @param inputStream 文件对象
     * @param key         对象键（Key）是对象在存储桶中的唯一标识。例如，在对象的访问域名 examplebucket-1250000000.cos.ap-guangzhou.myqcloud.com/images/picture.jpg 中，对象键为 images/picture.jpg
     * @return
     */
    public static String uploadFileV1(InputStream inputStream, String key) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(0);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, objectMetadata);
        Upload upload = COSClientInit.transferManager.upload(putObjectRequest);
        URL url = COSClientInit.CreateCOSClient().generatePresignedUrl(bucketName, key, new Date(System.currentTimeMillis() + 5 * 60 * 10000), HttpMethodName.PUT);
        String substring = url.toString().substring(0, url.toString().indexOf("?"));

        return substring;

    }

    /**
     * 下载腾讯云文件到本地磁盘
     *
     * @param bucketName   存储桶名称
     * @param key          对象键（Key）是对象在存储桶中的唯一标识。例如，在对象的访问域名 examplebucket-1250000000.cos.ap-guangzhou.myqcloud.com/images/picture.jpg 中，对象键为 images/picture.jpg
     * @param localTmpPath 本地存储目录
     * @return
     */
    public static ObjectMetadata downFile(String bucketName, String key, String localTmpPath) {
        File downFile = new File(localTmpPath);
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        return COSClientInit.CreateCOSClient().getObject(getObjectRequest, downFile);
    }

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param key        指定被删除的文件在 COS 上的路径，即对象键。例如对象键为folder/picture.jpg，则表示删除位于 folder 路径下的文件 picture.jpg
     */
    public static void deleteFile(String bucketName, String key) {
        COSClientInit.CreateCOSClient().deleteObject(bucketName, key);
    }
}
