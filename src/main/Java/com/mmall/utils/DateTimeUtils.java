package com.mmall.utils;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import java.util.Date;

//joda-time
public class DateTimeUtils {
    //str->Date
    //Date->str
    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Date strToDate(String dateTimeStr,String formatStr) {

        DateTimeFormatter dateTimeFormat= DateTimeFormat.forPattern(dateTimeStr);
        DateTime dateTime=dateTimeFormat.parseDateTime(formatStr);
        return dateTime.toDate();

    }

    public static String dateToStr(Date date, String formatStr){
        if (date==null){
            return  StringUtils.EMPTY;
        }
        DateTime dateTime=new DateTime();


        return dateTime.toString(formatStr);
    }

    public static Date strToDate(String dateTimeStr){
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(STANDARD_FORMAT);
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
        return dateTime.toDate();
    }

    public static String dateToStr(Date date){
        if(date == null){
            return StringUtils.EMPTY;
        }
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(STANDARD_FORMAT);
    }

}
