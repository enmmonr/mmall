package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.utils.FtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("iFileService")
public class IFileServiceImpl implements IFileService {
    private Logger logger= LoggerFactory.getLogger(IFileServiceImpl.class);
    @Override
    public String upload(MultipartFile multipartFile, String path) {
        //获取文件名
        String fileName=multipartFile.getOriginalFilename();

        //获取文件扩展名
        String fileExtensionName=fileName.substring(fileName.lastIndexOf(".")+1);
        //设置文件上传后的名字
        String uploadName= UUID.randomUUID().toString()+fileExtensionName;
        logger.info("开始上传文件,上传文件的文件名:{},上传的路径:{},新文件名:{}",fileName,path,uploadName);

        //如果路径下不存在文件夹则创建
        File fileDir=new File(path);
        if(!fileDir.exists()){
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        //创建上传的文件
        File targetFile=new File(path,uploadName);
        try {
            //上传文件
            multipartFile.transferTo(targetFile);

            //将文件上传至ftp服务器
            FtpUtil.uploadFile(Lists.newArrayList(targetFile));
            //删除本地文件
            targetFile.delete();

        } catch (IOException e) {
            logger.error("文件上传失败");
            return null;
        }
        return targetFile.getName();
    }
}
