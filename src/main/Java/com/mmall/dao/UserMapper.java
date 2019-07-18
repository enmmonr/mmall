package com.mmall.dao;

import com.mmall.pojo.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    //判断用户名是否存在
    int checkUsername(String username);

    //验证 email是否存在
    int checkEmail(String email);
    //用户登录时从数据库中查找信息
    User userSelectLogin(@Param("username") String username,@Param("password") String password);
    //查找密保问题
    String selectQuestion(@Param("username") String username);
    //根据用户名和密保问题查找密保问题答案
    int selectAnswer(@Param("username")String username,@Param("question")String question,@Param("answer")String answer0);
    //忘记密码时密码重置
    int restPassword(@Param("username") String username,@Param("passwordNew") String passwordNew);
    //重置密码时校验密码
    int checkPasswordOld(@Param("password") String password,@Param("userId")Integer userId);
    //通过用户名id查找email
    int checkEmailById(@Param("email")String email,@Param("userId")Integer userId);



}