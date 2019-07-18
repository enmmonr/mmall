package com.mmall.service;

import com.mmall.common.ServerResponse;

import com.mmall.pojo.User;

public interface IUserService {

    //登录
    ServerResponse<User> login(String username, String password);

    //注册
    ServerResponse<String> register(User user);

    //校验username和Email是否已经存在
    ServerResponse<String> checkValid(String str,String type);
    //查找密保问题
    ServerResponse<String> forgetGetQuestion(String username);
    //查找并验证密保问题答案
    ServerResponse<String> forgetCheckAnswer(String username,String question,String answer);
    //忘记密码时重置密码
    ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken);
    //重置密码
    ServerResponse<String> restPassword(String passwordOld,String passwordNew,User user);
    //更新用户个人信息
    ServerResponse<User> updateInformation(User user);
    //获取当前用户信息
    ServerResponse<User> getInformation(Integer id);
    //验证是否为管理员
    ServerResponse checkAdminUser(User user);
}
