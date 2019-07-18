package com.mmall.controller.backend;

import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.pojo.User;
import com.mmall.service.IFileService;
import com.mmall.service.IProductService;
import com.mmall.service.IUserService;

import com.mmall.utils.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/manage/product")
public class ProductManageController {
    @Autowired
    private IUserService iUserService;
    @Autowired
    private IProductService iProductService;
    @Autowired
    private IFileService iFileService;


   // 新增OR更新产品
    @RequestMapping("save.do")
    @ResponseBody
    public ServerResponse addOrUpdateProduct(HttpSession session, Product product){
        //校验是否已经登录
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"当前用户未登录,请先登录");
        }
        //校验是否为管理员
        if (iUserService.checkAdminUser(user).isSuccess()){
            return iProductService.addOrUpdateProduct( product);
        }else {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }

    }

    @RequestMapping("set_sale_status.do")
    @ResponseBody
    //产品上下架
    public ServerResponse setSaleStatus(HttpSession session,Integer productId,Integer status){
        //校验是否已经登录
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"当前用户未登录,请先登录");
        }
        //校验是否为管理员
        if (iUserService.checkAdminUser(user).isSuccess()){
            return iProductService.setSaleStatus(productId,status);
        }else {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }

    }
    //产品详情
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse getDetail(HttpSession session,Integer productId){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请先登录");

        }
        if(iUserService.checkAdminUser(user).isSuccess()){
            return iProductService.manageProductDetail(productId);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
    }

    //产品list
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse getProductList(HttpSession session,@RequestParam(value = "pageNum" ,defaultValue = "1") Integer pageNum,@RequestParam(value = "pageSize" ,defaultValue = "10") Integer pageSize){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请先登录");

        }
        if(iUserService.checkAdminUser(user).isSuccess()){
            return iProductService.getProductList(pageNum,pageSize);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
    }

    //产品搜索
    @RequestMapping("search.do")
    @ResponseBody
    public ServerResponse search(HttpSession session,String productName, Integer productId, @RequestParam(value = "pageNum" ,defaultValue = "1") Integer pageMum, @RequestParam(value = "pageMum",defaultValue = "10") Integer pageSize){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        if (iUserService.checkAdminUser(user).isSuccess()){
            return iProductService.manageSearchProduct(pageMum,pageSize,productId,productName);

        }else{
            return ServerResponse.createByErrorMessage("不是管理员,无权限操作");
        }

    }

    @RequestMapping("upload.do")
    @ResponseBody
    //上传文件
    public ServerResponse upload(HttpSession session, HttpServletRequest request, @RequestParam(value = "upload_file" ,required = false) MultipartFile file){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        if (iUserService.checkAdminUser(user).isSuccess()){
            //获取文件上传真实地址
           String path= request.getSession().getServletContext().getRealPath("upload");
            //获取文件上传后的名字
            String targetName=iFileService.upload(file,path);
            //ftp上的文件地址
            String url= PropertiesUtil.getProperty("ftp.server.http.prefix")+targetName;
            //设置返回前端的数据
            Map fileMap= Maps.newHashMap();
            fileMap.put("uri",targetName);
            fileMap.put("url",url);
            return ServerResponse.createBySuccess(fileMap);

        }else{
            return ServerResponse.createByErrorMessage("不是管理员,无权限操作");
        }
    }

    //富文本图片上传
    @RequestMapping("richtext_img_upload.do")
    @ResponseBody()
    public Map richTextImgUpload(HttpSession session, @RequestParam(value = "upload_file" ,required = false)MultipartFile file, HttpServletRequest request, HttpServletResponse response){
        //富文本中对于返回值有自己的要求,我们使用是simditor所以按照simditor的要求进行返回
        Map map=Maps.newHashMap();
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            map.put("success",false);
            map.put("msg","未登录");
            return map;
        }

        if (iUserService.checkAdminUser(user).isSuccess()){
            //获取文件上传真实地址
            String path= request.getSession().getServletContext().getRealPath("upload");
            //获取文件上传后的名字
            String targetName=iFileService.upload(file,path);
            if (StringUtils.isBlank(targetName)){
                map.put("success",false);
                map.put("msg","上传失败");
                return map;
            }
            //ftp上的文件地址
            String url= PropertiesUtil.getProperty("ftp.server.http.prefix")+targetName;
            //设置返回前端的数据
            map.put("success",true);
            map.put("msg","上传成功");
            map.put("url",url);

            //添加浏览器头
            response.addHeader("Access-Control-Allow-Headers","X-File-Name");
            return map;

        }else{
            map.put("success",false);
            map.put("msg","不是管理员,无权限操作");
            return map;
        }
    }
}

