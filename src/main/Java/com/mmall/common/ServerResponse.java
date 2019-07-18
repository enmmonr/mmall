package com.mmall.common;

import java.io.Serializable;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
//保证序列化json对象时，如果为null的对象，key也会消失，防止横向越权
public class ServerResponse<T> implements Serializable {
    private int status;
    private String msg;
    private T data;

    //构造函数私有化，防止被调用，会通过静态方法提供调用
    private ServerResponse(int status) {
        this.status=status;
    }
    private ServerResponse(int status, T data){
        this.status=status;
        this.data=data;
    }
    private ServerResponse(int status,String msg)
    {
        this.status=status;
        this.msg=msg;
    }
    private ServerResponse(int status,String msg,T data)
    {
        this.status=status;
        this.msg=msg;
        this.data=data;
    }
    @JsonIgnore
    //使之不在序列化结果当中
    public boolean isSuccess()
    {
        return this.status==ResponseCode.SUCCESS.getCode();
    }

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

public static <T> ServerResponse<T> createBySuccess(){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode());
}
public static <T>ServerResponse<T> createBySuccessMessage(String msg)
{
    return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg);
}
public static <T> ServerResponse<T> createBySuccess(T data)
{
    return new ServerResponse<>(ResponseCode.SUCCESS.getCode(),data);
}
public static <T> ServerResponse <T> createBySuccess(String msg,T data)
{
    return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg,data);
}
public  static <T>ServerResponse<T> createByError()
{
    return  new ServerResponse<T>(ResponseCode.ERROR.getCode(),ResponseCode.ERROR.getDesc());
}
public static <T>ServerResponse<T> createByErrorMessage(String errorMessage)
{
    return new ServerResponse<T>(ResponseCode.ERROR.getCode(),errorMessage);
}
public static <T>ServerResponse<T> createByErrorCodeMessage(int errorCode,String errorMessage)
{
    return new ServerResponse<T>(errorCode,errorMessage);
}
}
