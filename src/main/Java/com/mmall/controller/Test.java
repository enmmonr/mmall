package com.mmall.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/test")
@Controller
public class Test {
    private Logger logger= LoggerFactory.getLogger(Test.class);
    @RequestMapping("testValue.do")
    @ResponseBody
    public String test(String str){
        logger.info("info");
        logger.warn("warn");
        logger.error("error");
        return "testValue:"+str;
    }
}
