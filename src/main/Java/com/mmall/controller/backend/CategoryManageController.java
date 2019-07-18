package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/*分类管理类*/
@Controller
@RequestMapping("/manage/category")

public class CategoryManageController {
    @Autowired
    private IUserService iUserService;
    @Autowired
    private ICategoryService iCategoryService;


    //添加分类
    @RequestMapping(value = "add_category.do")
    @ResponseBody
    public ServerResponse addCategory(HttpSession session, @RequestParam(value = "parentId",defaultValue ="0") Integer parentId, String categoryName){
    //校验当前用户是否是管理员
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请先登录");
        }
        if (iUserService.checkAdminUser(user).isSuccess()){
            return  iCategoryService.addCategory(parentId,categoryName);
        }else {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
    }

    //更新分类名字
    @RequestMapping(value = "set_category_name.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse setCategory(Integer categoryId, String categoryName, HttpSession session){
        //校验是否为管理员
        User user=(User)session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请先登录");
        }
        if (iUserService.checkAdminUser(user).isSuccess()){
            return iCategoryService.setCategoryName(categoryId,categoryName);
        }else {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
    }

//  获取平级品类子节点信息
    @RequestMapping("get_category.do")
    @ResponseBody
    public ServerResponse getCategory(HttpSession session ,@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        //校验是否为管理员
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
             return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请先登录");
        }
        if(iUserService.checkAdminUser(user).isSuccess()){
            return iCategoryService.getCategory(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }

    }

    //递归查询本节点的id及孩子节点的id
    @ResponseBody
    @RequestMapping("get_children_category.do")
    public ServerResponse getCategoryAndDeepChildrenCategory(HttpSession session ,@RequestParam(value = "categoryId" ,defaultValue = "0")Integer categoryId){
        //校验是否为管理员
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
           return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请先登录");
        }
        if(iUserService.checkAdminUser(user).isSuccess()){
            return iCategoryService.getCategoryAndDeepChildrenCategory(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }

    }

}
