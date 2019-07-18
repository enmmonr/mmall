package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.utils.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class IUserServiceImpl implements IUserService {
    //注入dao层对象
    @Autowired
    private UserMapper userMapper;
    @Override
    public ServerResponse<User> login(String username, String password) {
        //判断用户名是否存在
        int resultCount=userMapper.checkUsername(username);
        if (resultCount==0)
        {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        String MD5Password=MD5Util.MD5EncodeUtf8(password);

        //判断用户名密码是否正确
        User user=userMapper.userSelectLogin(username,MD5Password);
        if(user==null)
        {
            return ServerResponse.createByErrorMessage("密码错误");
        }
        //密码匹配登陆成功时，将密码置空
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功",user);

    }
    /*
    *用户注册*/
    public ServerResponse<String> register(User user){
        ServerResponse responseValid=this.checkValid(user.getUsername(), Const.USERNAME);
        if(!responseValid.isSuccess()){
            return responseValid;
        }

        responseValid=this.checkValid(user.getEmail(),Const.EMAIL);
        if (!responseValid.isSuccess()){
            return responseValid;
        }
        //设置为普通用户
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //将密码用MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        //向数据库中添加注册信息
        int registerResult=userMapper.insert(user);
        if(registerResult==0)
        {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");

    }

    /*用于验证用户名与email是否已存在
     *type表示传入的类型,根据类型执行后面的判断方法
     * StringUtils.isNotBlank()  后面的空格无效 ，与StringUtils.isNotEmpty()相反
     */
    public ServerResponse<String> checkValid(String str, String type) {
        //       验证用户名是否存在

        if(StringUtils.isNotBlank(type)) {
            if (Const.USERNAME.equals(type)) {
                int usernameResult=userMapper.checkUsername(str);
                if(usernameResult>0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            //验证邮箱是否已存在
            if(Const.EMAIL.equals(type)){
                int emailResult=userMapper.checkEmail(str);
                if(emailResult>0){
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        }else{
            //参数既不是username也不是email
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    @Override
    /*查找密保问题*/
    public ServerResponse<String> forgetGetQuestion(String username) {
        ServerResponse validResponse=this.checkValid(username,Const.USERNAME);

        if(validResponse.isSuccess()){
            //判断到当前用户不存在
            return ServerResponse.createByErrorMessage("当前用户不存在");
        }
        String question=userMapper.selectQuestion(username);
        //将取回的数据去空格
        if(StringUtils.isNotBlank(question))
        {
            return ServerResponse.createBySuccess(question);
        }
        return  ServerResponse.createBySuccessMessage("没有密保问题");

    }
    /*验证密保问题答案*/
    @Override
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
       int resultCount= userMapper.selectAnswer(username,question,answer);
       if(resultCount>0)
       {
/*           生成token
           token 的作用
           1.防止表单重复提交
           2.用作身份验证*/
           String forgetToken= UUID.randomUUID().toString();
           //将token放入本地缓存
           TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);
           return  ServerResponse.createBySuccess(forgetToken);
       }
        return ServerResponse.createByErrorMessage("答案错误");
    }
    /*忘记密码时修改密码*/
    @Override
    public ServerResponse<String> forgetRestPassword(String username, String passwordNew, String forgetToken) {
        /*校验token*/
        if(StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("token需要传递");

        }
        //校验用户名是否存在
        if(checkValid(username,Const.USERNAME).isSuccess())
        {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        //            从内存中获取token
        String token=TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token无效或过期");
        }
        /*判断两个token是否相等*/
        if(StringUtils.equals(token,forgetToken)){
            String password=MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount=userMapper.restPassword(username,password);
            if(rowCount>0){
                return ServerResponse.createBySuccessMessage("密码修改成功");
            }
        }else {
            return ServerResponse.createByErrorMessage("token错误,请重新获取");
        }
        return ServerResponse.createByErrorMessage("密码修改失败");
    }

    /*重置密码*/
    public  ServerResponse<String> restPassword(String passwordOld,String passwordNew,User user)
    {
        //为防止横向越权，需根据用户id检验用户的旧密码
        int rowCount=userMapper.checkPasswordOld(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(rowCount==0){
            return ServerResponse.createByErrorMessage("旧密码不正确");
        }
        //重置密码
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount=userMapper.updateByPrimaryKeySelective(user);
        if (updateCount>0){
            return ServerResponse.createBySuccessMessage("新密码设置成功");
        }
        return ServerResponse.createBySuccessMessage("新密码设置失败");
    }

    /*更新用户信息*/
    @Override
    public ServerResponse<User> updateInformation(User user) {
        //验证邮箱是否被其他用户名占用
        //username是不能被更新的
        //email也要进行一个校验,校验新的email是不是已经存在,并且存在的email如果相同的话,不能是我们当前的这个用户的.
        int emailCount=userMapper.checkEmailById(user.getEmail(),user.getId());
        if(emailCount>0){
            return ServerResponse.createByErrorMessage("邮箱已被占用");
        }
        User updateUser=new User();
        //只更新需要更新的数据
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setId(user.getId());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());
        //更新用户信息
        int updateCount=userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount>0){
            return ServerResponse.createBySuccess("用户信息更新成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("用户信息更新失败");
    }

//    获取当前登录用户的详细信息，并强制登录
    @Override
    public ServerResponse<User> getInformation(Integer id) {
        User userResult=userMapper.selectByPrimaryKey(id);
        if(userResult==null){
            return ServerResponse.createByErrorMessage("无当前用户信息");

        }
        userResult.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("查找当前用户信息成功",userResult);
    }
//判断用户是否为管理员
    public ServerResponse checkAdminUser(User user){
        if(user==null ||user.getRole().intValue()!=Const.Role.ROLE_ADMIN){
            return ServerResponse.createByError();
        }
        return ServerResponse.createBySuccess();
    }
}
