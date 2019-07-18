package com.mmall.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.github.pagehelper.PageInfo;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import com.mmall.vo.OrderVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private IOrderService iOrderService;

    private Logger logger= LoggerFactory.getLogger(OrderController.class);


    //创建订单
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpSession session,Integer shippingId){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        return iOrderService.create(user.getId(),shippingId);

    }
    //获取订单的商品信息
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public  ServerResponse getOrderCartProduct(HttpSession session){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }


    //订单List
    @ResponseBody
    @RequestMapping("list.do")
    public ServerResponse<PageInfo> list(@RequestParam(defaultValue = "1" ,value="pageNum") Integer pageNum, @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize, HttpSession session){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.list(pageNum,pageSize,user.getId());

    }

    //订单详情
    @ResponseBody
    @RequestMapping("detail.do")
    public ServerResponse<OrderVo> detail(HttpSession session,Long orderNo){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.detail(user.getId(),orderNo);

    }

    //取消订单
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpSession session,Long orderNo){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancel(user.getId(),orderNo);
    }























    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpSession session, HttpServletRequest request,Long orderNo){
       User user= (User) session.getAttribute(Const.CURRENT_USER);
       if(user==null){
           return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
       }
       String path=request.getSession().getServletContext().getRealPath("upload");
      return iOrderService.pay(user.getId(),orderNo,path);

    }

    @ResponseBody
    @RequestMapping("alipay_callback.do")
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> map=new HashMap();
        //取出通知回调参数
        Map<String,String[]> requestParams=request.getParameterMap();
        for(Iterator iterator=requestParams.keySet().iterator();iterator.hasNext();){
            String name=(String)iterator.next();
            String[] params=(String[])requestParams.get(name);

            String values="";
            for(int i=0;i<params.length;i++){
                values=(i==params.length-1)?values+params[i]:values+params[i]+",";
            }
            map.put(name,values);
        }
        logger.info("支付宝回调：sign{},trade_status{},参数{}",map.get("sign"),map.get("trade_status"),map.toString());

        //验证回调的正确性，是不是支付宝发的，并且需要避免重复通知
        ////除去sign、sign_type两个参数，并对剩余参数验签，sign参数在AlipaySignature中会被getSignCheckContentV2()方法移除
        map.remove("sign_type");
        try {
            boolean rsa2Check= AlipaySignature.rsaCheckV2(map, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
            if(!rsa2Check){
                return ServerResponse.createByErrorMessage("非法参数，继续将报警");
            }
        } catch (AlipayApiException e) {
            logger.error("非法参数",e);
        }
    //校验数据
       ServerResponse serverResponse= iOrderService.aliCallback(map);
        if(serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }


    @ResponseBody
    @RequestMapping("query_order_pay_status.do")
    //查询订单支付状态
    public ServerResponse<Boolean> queryOrderPayStatus(Long orderNo,HttpSession session){
        User user= (User) session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        ServerResponse serverResponse=iOrderService.queryOrderPayStatus(orderNo,user.getId());
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
}
