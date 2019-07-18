package com.mmall.utils;

import java.math.BigDecimal;

public class BigDecimalUtil {

    private BigDecimalUtil(){

    }
    public static BigDecimal add(double a1,double a2){
        BigDecimal b1=new BigDecimal(Double.toString(a1));
        BigDecimal b2=new BigDecimal(Double.toString(a2));
        return b1.add(b2);
    }
    public static BigDecimal sub(double a1,double a2){
        BigDecimal b1=new BigDecimal(Double.toString(a1));
        BigDecimal b2=new BigDecimal(Double.toString(a2));
        return b1.subtract(b2);
    }
    public static BigDecimal mul(double a1,double a2){
        BigDecimal b1=new BigDecimal(Double.toString(a1));
        BigDecimal b2=new BigDecimal(Double.toString(a2));
        return b1.multiply(b2);
    }
    //除法要考虑除不尽的情况
    public static BigDecimal divide(double a1,double a2){
        BigDecimal b1=new BigDecimal(Double.toString(a1));
        BigDecimal b2=new BigDecimal(Double.toString(a2));
        return b1.divide(b2,2,BigDecimal.ROUND_HALF_UP);//四舍五入，保留两位小数
    }
}
