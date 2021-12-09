package com.debug.kill.server.utils;

/**
 * @author:ZHAOYONGHENG
 * @date:2021/12/3
 * @version:1.0.0
 */

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.retry.RetryPolicy;
import com.qcloud.cos.transfer.*;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯云文件上传
 *
 * @author:ZHAOYONGHENG
 * @date:2021/12/3
 * @version:1.0.0
 */
public class CosUtils {
    static String bucketName = "zhaoyongheng1"; //桶的名称
    //    static String key = "upload_inage/2222.png";         //上传到云上路径
    static String key = "upload_image1/aabb.jpg";         //上传到云上路径
    static String region = "ap-nanjing";//区域南京
    static String appId = "1308629741"; //APPID
    //    static COSCredentials cred = null;
    static TransferManager transferManager = null;
    static COSClient cosClient = null;

    //手动创建线程池

    private static ArrayBlockingQueue queue = new ArrayBlockingQueue(8, true);

    private static ThreadPoolExecutor.CallerRunsPolicy policy = new ThreadPoolExecutor.CallerRunsPolicy();

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 8, 10, TimeUnit.SECONDS
            , queue, policy);

    static {
        // 1 初始化用户身份信息(secretId, secretKey)
        //SecretId 是用于标识 API 调用者的身份
        String SecretId = "AKID5zd07yZtwiDQO5WgZCZfU2MSIsBDyUkJ";
        //SecretKey是用于加密签名字符串和服务器端验证签名字符串的密钥
        String SecretKey = "LagY9WeVAbokPnulMhlZ6ML4jGXZOzJO";
        COSCredentials cred = new BasicCOSCredentials(SecretId, SecretKey);

        // 2 设置bucket的区域,
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 设置最大重试次数为 4 次
        clientConfig.setMaxErrorRetry(4);

        RetryPolicy myRetryPolicy = new OnlyIOExceptionRetryPolicy();
        // 设置自定义的重试策略
        clientConfig.setRetryPolicy(myRetryPolicy);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成cos客户端
        cosClient = new COSClient(cred, clientConfig);
        // 指定要上传到 COS 上的路径
//        ExecutorService threadPool = Executors.newFixedThreadPool(32);
//        executor.execute(new Thread());
        // 传入一个 threadpool, 若不传入线程池, 默认 TransferManager 中会生成一个单线程的线程池。
        transferManager = new TransferManager(cosClient, executor);
    }

    public static void main(String[] args) {
        //创建存储桶
//        createBucket();
        //上传
//        upload();
        //下载
        download();
        //删除
//        delete();
//        cosClient.shutdown();
    }

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm ss");


    /**
     * 创建存储桶
     */
    public static void createBucket() {
        //存储桶名称，格式：BucketName-APPID
        String bucket = bucketName + "-" + appId;
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucket);
// 设置 bucket 的权限为 Private(私有读写)、其他可选有 PublicRead（公有读私有写）、PublicReadWrite（公有读写）
        createBucketRequest.setCannedAcl(CannedAccessControlList.PublicReadWrite);
        try {
            Bucket bucketResult = cosClient.createBucket(createBucketRequest);
            List<Bucket> buckets = cosClient.listBuckets();
            for (Bucket bucketElement : buckets) {
                String bucketName = bucketElement.getName();
                String bucketLocation = bucketElement.getLocation();
                System.out.println(bucketName);
                System.out.println(bucketLocation);
            }
        } catch (CosServiceException serverException) {
            serverException.printStackTrace();
        } catch (CosClientException clientException) {
            clientException.printStackTrace();
        }
    }


    /**
     * 上传
     */
    public static String upload() {
        try {
            System.out.println("上传开始时间:" + sdf.format(new Date()));
            // .....(提交上传下载请求, 如下文所属)
            // bucket 的命名规则为{name}-{appid} ，此处填写的存储桶名称必须为此格式
            String bucket = bucketName + "-" + appId;
            //本地文件路径
            File localFile = new File("D:/data/file/dd.png");
            //str1为待转换的字符串
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
//                    InputStream is = new ByteArrayInputStream(os.toByteArray());
//                    ObjectMetadata objectMetadata = new  ObjectMetadata();
//                    // 设置文件大小大于等于10MB才使用分块上传
//                    TransferManagerConfiguration transferManagerConfiguration = new TransferManagerConfiguration();
//                    transferManagerConfiguration.setMultipartUploadThreshold(10*1024*1024);
//                    transferManager.setConfiguration(transferManagerConfiguration);
            Upload upload = transferManager.upload(putObjectRequest);
            showTransferProgress(upload);
            // 同步打印上传进度
            // 可以通过子线程调用这个函数，但是要注意要在 waitForUploadResult 前启动子线程，否则因为upload已完成看不到进度。
            // 异步（如果想等待传输结束，则调用 waitForUploadResult）
            UploadResult uploadResult = upload.waitForUploadResult();
            //同步的等待上传结束waitForCompletion
//                    upload.waitForCompletion();
            System.out.println("上传结束时间:" + sdf.format(new Date()));
            System.out.println("上传成功");
            //获取上传成功之后文件的下载地址
            URL url = cosClient.generatePresignedUrl(bucketName + "-" + appId, key, new Date(System.currentTimeMillis() + 5 * 60 * 10000), HttpMethodName.PUT);
            String substring = url.toString().substring(0, url.toString().indexOf("?"));
            System.out.println(url);
            System.out.println(substring);
        } catch (Throwable tb) {
            System.out.println("上传失败");
            tb.printStackTrace();
        } finally {
            // 关闭 TransferManger
            transferManager.shutdownNow();
        }
        // 关闭客户端(关闭后台线程)
        cosClient.shutdown();
        return null;

    }

    // 编写自己的上传时打印上传进度的回调函数
    static void showTransferProgress(Transfer transfer) {
        System.out.println(transfer.getDescription());
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            TransferProgress progress = transfer.getProgress();
            long so_far = progress.getBytesTransferred();
            long total = progress.getTotalBytesToTransfer();
            double pct = progress.getPercentTransferred();
            System.out.printf("[%d / %d] = %.02f%%\n", so_far, total, pct);
        } while (transfer.isDone() == false);
        System.out.println(transfer.getState());
    }

    /**
     * 下载
     */
    public static void download() {
        try {
            //下载到本地指定路径
            File localDownFile = new File("downLoad/Images/"+key);
            if (!localDownFile.exists()){
                localDownFile.getParentFile().mkdirs();
            }
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName + "-" + appId, key);
            // 下载文件
            Download download = transferManager.download(getObjectRequest, localDownFile);
            // 等待传输结束（如果想同步的等待上传结束，则调用 waitForCompletion）
            download.waitForCompletion();
            System.out.println("下载成功");
        } catch (Throwable tb) {
            System.out.println("下载失败");
            tb.printStackTrace();
        } finally {
            // 关闭 TransferManger
            transferManager.shutdownNow();
        }
    }

    /**
     * 删除
     */
    public static void delete() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 指定要删除的 bucket 和路径
                try {
                    cosClient.deleteObject(bucketName + "-" + appId, key);
                    System.out.println("删除成功");
                } catch (Throwable tb) {
                    System.out.println("删除文件失败");
                    tb.printStackTrace();
                }
            }
        }).start();
    }
}