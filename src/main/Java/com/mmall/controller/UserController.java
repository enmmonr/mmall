package com.mmall.controller;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/")
public class UserController {
    @Autowired
    private IUserService iUserService;
    @RequestMapping(value = "login.do",method = RequestMethod.POST )
    @ResponseBody
    public ServerResponse<User> login (String username, String password, HttpSession session)
    {
        ServerResponse<User> response=iUserService.login(username,password);
        //将用户信息放入session
        if(response.isSuccess())
        {
           session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }
    /*登出*/
    @RequestMapping(value = "logout.do" ,method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> logout(HttpSession session)
    {
        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess();
    }
    /*注册*/
    @RequestMapping(value = "register.do",method = RequestMethod.POST )
    @ResponseBody
    public ServerResponse<String> register(User user){
        ServerResponse response=iUserService.register(user);
        return response;
    }
    /*获取登录用户信息*/
    @ResponseBody
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    public ServerResponse<User> getUserInfo(HttpSession session){
        //从session中获取当前登录用户的信息
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null)
        {
            return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户的信息");
        }
        return ServerResponse.createBySuccess(user);

    }
    /*忘记密码时验证密保问题*/

    @ResponseBody
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    public ServerResponse<String> forgetGetQuestion(String username){
        return iUserService.forgetGetQuestion(username);

    }

    /*验证密保问题答案是否正确*/
    @ResponseBody
    @RequestMapping(value = "forget_check _answer.do",method = RequestMethod.POST)
public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer){

    return  iUserService.forgetCheckAnswer(username,question,answer);
}
    /*忘记密码时重置密码*/
    @ResponseBody
    @RequestMapping(value = "forget_rest_password.do",method = RequestMethod.POST)
    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken)
    {
        return iUserService.forgetRestPassword(username,passwordNew,forgetToken);
    }

    /*重置密码*/
    @ResponseBody
    @RequestMapping(value = "rest_password.do",method = RequestMethod.POST)
    public ServerResponse<String> restPassword(HttpSession session ,String passwordOld,String passwordNew){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.restPassword(passwordOld,passwordNew,user);
    }

    /*更新用户信息,用户名不可以改变*/
    @ResponseBody
    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    public  ServerResponse<User> update_information(HttpSession session,User user){
        User currentUser= (User) session.getAttribute(Const.CURRENT_USER);
        if(currentUser==null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        //防止横向越权
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());

        ServerResponse<User> response=iUserService.updateInformation(user);
        if(response.isSuccess())
        {
            //返回的数据中没有username，所以需要设定其username
            response.getData().setUsername(currentUser.getUsername());
            //将返回来的更新好的数据放入session
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }


    /*获取当前登录用户的详细信息，并强制登录*/
    @ResponseBody
    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    public ServerResponse<User> get_information(HttpSession session){
        //从session中获取当前用户
        User user= (User) session.getAttribute(Const.CURRENT_USER);
//        判断用户是否已经登录
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录,需要强制登录status=10");
        }
        return iUserService.getInformation(user.getId());

    }
}
