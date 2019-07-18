package com.mmall.utils;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class FtpUtil {
    private static Logger logger= LoggerFactory.getLogger(FtpUtil.class);
    private static  String ftpIp=PropertiesUtil.getProperty("ftp.server.ip");
    private static String fipUser=PropertiesUtil.getProperty("ftp.user");
    private static String ftpPass=PropertiesUtil.getProperty("ftp.pass");

    public FtpUtil(String ip,int port,String username,String password){
        this.ip=ip;
        this.port=port;
        this.username=username;
        this.password=password;
    }
    public static boolean uploadFile(List<File> fileList) throws IOException {
        FtpUtil ftpUtil=new FtpUtil(ftpIp,21,fipUser,ftpPass);
        logger.info("开始连接ftp服务器");
        boolean result=ftpUtil.uploadFile("img",fileList);
        logger.info("文件上传结束，上传结果{}");
        return result;

    }
    private  boolean uploadFile(String remotePate,List<File> fileList)
            throws IOException {
        boolean uploaded=true;
        FileInputStream fileInputStream=null;
        if(connectServer(this.ip,this.port,this.username,this.password)){
            //连接成功
            try {
                //切换工作目录
                ftpClient.changeWorkingDirectory(remotePate);
                //设置缓冲区大小
                ftpClient.setBufferSize(1024);
                //设置编码格式
                ftpClient.setControlEncoding("UTF-8");
                //设置上传文件的类型为二进制
                ftpClient.setFileType(ftpClient.BINARY_FILE_TYPE);
                //打开本地被动模式
                ftpClient.enterLocalPassiveMode();

                //上传文件到服务器
                for (File file:fileList
                     ) {
                    fileInputStream=new FileInputStream(file);
                    ftpClient.storeFile(remotePate,fileInputStream);
                }
            } catch (IOException e) {
                logger.error("文件上传失败");
                uploaded=false;
            }finally {
                fileInputStream.close();
                ftpClient.disconnect();
            }

        }
            return uploaded;
    }

    //连接ftp服务器
    private  boolean connectServer(String ip,int port,String username,String password){
        boolean isSuccess=false;
        ftpClient=new FTPClient();
        try {
            ftpClient.connect(ip,port);

            isSuccess=ftpClient.login(username,password);

        } catch (IOException e) {
           logger.error("连接服务器异常");
        }
        return isSuccess;
    }


    private String ip;
    private int port;
    private String username;
    private String password;
    private FTPClient ftpClient;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
       username = username;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
