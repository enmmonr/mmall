package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.service.IOrderService;
import com.mmall.utils.BigDecimalUtil;
import com.mmall.utils.DateTimeUtils;
import com.mmall.utils.FtpUtil;
import com.mmall.utils.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service("iOrderService")
public class IOrderServiceImpl implements IOrderService {
    private Logger log= LoggerFactory.getLogger(IOrderService.class);

    private static  AlipayTradeService tradeService;
    static {

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private  OrderItemMapper orderItemMapper;
    @Autowired
    private ShippingMapper shippingMapper;

    public ServerResponse create(Integer userId,Integer shippingId){
        //从购物车中获取已经被勾选的商品
        List<Cart> cartList=cartMapper.selectCheckedByUserId(userId);
        //计算这个订单的总价
        ServerResponse serverResponse=this.getCartOrderItem(cartList,userId);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList=(List<OrderItem>) serverResponse.getData();
        BigDecimal payment=this.getOrderTotalPrice(orderItemList);

        //生成订单
        Order order=assembleOrder(userId,shippingId,payment);
        if(order==null){
            return ServerResponse.createByErrorMessage("订单生成错误");
        }
        //批量设置订单号
        for(OrderItem orderItem:orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //mybatis批量插入
        orderItemMapper.batchinsert(orderItemList);

        //生成成功，减少库存
        this.reduceQuantity(orderItemList);
        //清空购物车
        this.cleanCart(cartList);
        //给前端返回数据
        OrderVo orderVo=this.assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);

    }


    //获取订单的商品信息
    @Override
    public ServerResponse getOrderCartProduct(Integer userId) {
        //定义一个vo,用于最后将数据返回前端
       OrderProductVo orderProductVo=new OrderProductVo();
        //从购物车中获取商品信息
        List<Cart> cartList=cartMapper.selectCheckedByUserId(userId);
        //检查购物车中的商品状态
        ServerResponse serverResponse=this.getCartOrderItem(cartList,userId);
        if (!serverResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("未找到商品信息");
        }
        //将购物车中的商品信息存入集合
        List<OrderItem> orderItemList= (List<OrderItem>) serverResponse.getData();
        List<OrderItemVo> orderItemVoList=Lists.newArrayList();

        BigDecimal payment=new BigDecimal("0");
        for (OrderItem orderItem:orderItemList){
            payment=BigDecimalUtil.add(orderItem.getTotalPrice().doubleValue(),payment.doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setProductTotalPrice(payment);



        return ServerResponse.createBySuccess(orderProductVo);
    }

    //订单list
    public ServerResponse<PageInfo> list(Integer pageNum,Integer pageSize,Integer userId){
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList=orderMapper.selectOrderByUserId(userId);
        List<OrderVo> orderVoList=this.assembleOrderVoList(userId,orderList);
        PageInfo pageInfo=new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);

    }

    //订单详情
    public ServerResponse<OrderVo> detail(Integer userId,Long orderNo){
        Order order=orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if(order!=null){
            List<OrderItem> orderItemList=orderItemMapper.selectByOrderNoUserId(orderNo,userId);
            OrderVo orderVo=assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
    return ServerResponse.createByErrorMessage("未找到该订单");

    }
    //订单取消
    public ServerResponse cancel(Integer userId,Long orderNo){
        Order order=orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order==null){
            return ServerResponse.createByErrorMessage("没有该订单");
        }
        if(order.getStatus()!=Const.OrderStatuesEnum.PAID.getCode()){
            return ServerResponse.createByErrorMessage("该订单已付款，不可取消");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatuesEnum.CANCELED.getCode());

        int row = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(row > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }









    private List<OrderVo> assembleOrderVoList(Integer userId,List<Order> orderList){
        List<OrderVo> orderVoList=Lists.newArrayList();
        for(Order order:orderList){
            List<OrderItem> orderItemList=Lists.newArrayList();
            if(userId==null){
                //管理员查询不需要userId
                orderItemList=orderItemMapper.selectByOrderNo(order.getOrderNo());
            } else{
                orderItemList=orderItemMapper.selectByOrderNoUserId(order.getOrderNo(),userId);
            }
            orderVoList.add(assembleOrderVo(order,orderItemList));
        }
    return orderVoList;
    }

    //返回前端数据装配
    private OrderVo assembleOrderVo(Order order ,List<OrderItem> orderItemList){
        OrderVo orderVo=new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());
        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatuesEnum.codeOf(order.getStatus()).getValue());
        orderVo.setShippingId(order.getShippingId());
        Shipping shipping=shippingMapper.selectByPrimaryKey(order.getShippingId());
        if(shipping!=null){
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }
        orderVo.setPaymentTime(DateTimeUtils.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtils.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtils.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtils.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtils.dateToStr(order.getCloseTime()));
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        List<OrderItemVo> orderItemVoList=Lists.newArrayList();
        for (OrderItem orderItem:orderItemList
             ) {
            OrderItemVo orderItemVo=assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;

    }
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo=new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCreateTime(DateTimeUtils.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }

    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo=new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        return shippingVo;
    }

    //清空购物车
    private void cleanCart(List<Cart> cartList){
        for (Cart cart:cartList
             ) {
            cartMapper.deleteByPrimaryKey(cart.getId());
        }


    }
    //减少库存
    private void reduceQuantity(List<OrderItem> orderItemList){
        for(OrderItem orderItem:orderItemList){
            Product product=productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock()-orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);

        }

    }
    private ServerResponse getCartOrderItem(List<Cart> cartList,Integer userId){
        //new一个orderItem的集合
        List<OrderItem> orderItemList=Lists.newArrayList();
        //判断购物车中是否有商品
        if(CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        //判断购物车中的商品状态（是否在售，库存）
        for(Cart cartItem:cartList){
            Product product=productMapper.selectByPrimaryKey(cartItem.getProductId());
            if(product.getStatus()!=Const.ProductStatusEnum.ON_SALE.getCode()){
                return ServerResponse.createByErrorMessage(product.getName()+"该商品已下架");
            }
            //校验库存
            if(cartItem.getQuantity()>product.getStock()){
                return ServerResponse.createByErrorMessage(product.getName()+"该商品库存不足");
            }
            //组装OrderItem
            OrderItem orderItem=new OrderItem();
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());//单价
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity()));
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }
    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal payment){
        Order order=new Order();
        long orderNo=this.generateOrderNo();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPayment(payment);
        order.setShippingId(shippingId);
        order.setStatus(Const.OrderStatuesEnum.NO_PAY.getCode());//订单支付状态
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        order.setPostage(0);
        //向数据库中插入数据
        int rowCount = orderMapper.insert(order);
        if(rowCount > 0){
            return order;
        }
        return null;
    }
    //购物车状态检查
    //计算订单总价
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal totalPrice=new BigDecimal("0");
        for(OrderItem orderItem:orderItemList){
            totalPrice=BigDecimalUtil.add(orderItem.getTotalPrice().doubleValue(),totalPrice.doubleValue());
        }
        return totalPrice;
    }
    //生成订单
    //生成订单号
    private Long generateOrderNo(){
        long currentTime=System.currentTimeMillis();
        return currentTime+new Random().nextInt(100);

    }



    //后台订单list
    @Override
    public ServerResponse<PageInfo> manageList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList=orderMapper.selectAllOrder();
        List<OrderVo> orderVoList=this.assembleOrderVoList(null,orderList);
        PageInfo pageInfo=new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    //后台订单详情
    public ServerResponse<OrderVo> manageDetail(Long orderNo){
        //根据订单号查询订单信息
        Order order=orderMapper.selectByOrderNo(orderNo);
        if(order!=null){
        //获取此order中的所有商品信息
        List<OrderItem> orderItemList=orderItemMapper.selectByOrderNo(orderNo);
        //装配OrderVo中的OrderItemVo
        OrderVo orderVo=this.assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("没有该订单");

    }
    //订单发货
    public ServerResponse<String> sendGoods(Long orderNo){
        Order order=orderMapper.selectByOrderNo(orderNo);
        if(order!=null){
            if(order.getStatus() == Const.OrderStatuesEnum.PAID.getCode())
            order.setStatus(Const.OrderStatuesEnum.SHIPPED.getCode());
            order.setSendTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);
            return ServerResponse.createBySuccessMessage("发货成功");
        }
        return ServerResponse.createByErrorMessage("发货失败");
    }
    //按订单号查询
    public ServerResponse<PageInfo> search(Long orderNo,Integer pageNum,Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        Order order=orderMapper.selectByOrderNo(orderNo);
        if(order!=null){
        List<OrderItem> orderItemList=orderItemMapper.selectByOrderNo(orderNo);
        OrderVo orderVo=this.assembleOrderVo(order,orderItemList);

        PageInfo pageInfo=new PageInfo(Lists.newArrayList(order));
        pageInfo.setList(Lists.newArrayList(orderVo));
        return ServerResponse.createBySuccess(pageInfo);
        }
        return ServerResponse.createByErrorMessage("没有找到该订单");
    }






    @Override
    public ServerResponse pay(Integer userId, Long orderNo, String path) {
        Map<String,String> map=new HashMap<>();

        Order order=orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order==null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        map.put("orderNo",String.valueOf(order.getOrderNo()));

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo =order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("快乐牧商城扫码支付，订单编号为：").append(outTradeNo).toString();
        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空app，则默认为与支付宝签约的商户的PID，也就是id对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("购买门店："+subject).append("订单总金额"+order.getPayment()).toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        List<OrderItem> orderList=orderMapper.selectOrderItemListByUserIdAndOrderNo(userId,orderNo);

            for (OrderItem orderItem:orderList
                 ) {
                // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
                GoodsDetail goods1 =
                        GoodsDetail.newInstance(orderItem.getProductId().toString(),orderItem.getProductName(), BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),orderItem.getQuantity());
                // 创建好一个商品后添加至商品明细列表
                goodsDetailList.add(goods1);
            }





        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);


        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                File file=new File(path);
                if(!file.exists()){
                    file.setWritable(true);
                    file.mkdirs();
                }

                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                // 需要修改为运行机器上的路径
                String qrPath = String.format(path+"/qr-%s.png", response.getOutTradeNo());//生成的二维码的路径
                String qrCodeName=String.format("qr-%s", response.getOutTradeNo());//生成的二维码的名字
                //生成二维码
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);
                //上传二维码到服务器
                File targetFile=new File(path,qrCodeName);
                try {
                    FtpUtil.uploadFile(Lists.newArrayList(targetFile));
                }catch (IOException e){
                    log.error("二维码上传失败",e);
                }
                //二维码的url
                String qrUrl=PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFile.getName();
                map.put("qrUrl",qrUrl);
                log.info("qrPath:" + qrPath);

                 return ServerResponse.createBySuccess(map);

            case FAILED:
                log.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");

        }
    }




    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
}

//支付宝回调数据校验
    public ServerResponse aliCallback(Map<String,String> params){
        //获取数据中的订单号
        Long orderNo= Long.parseLong(params.get("out_trade_no"));
        //支付宝交易号
        String tradeNo=params.get("trade_no");
        //交易状态
        String tradeStatus=params.get("trade_status");
        //验证是否存在此订单
        Order order=orderMapper.selectByOrderNo(orderNo);
        if(order==null){
            return ServerResponse.createByErrorMessage("不存在此订单");
        }
        if(order.getStatus()>= Const.OrderStatuesEnum.PAID.getCode()){
            return ServerResponse.createByErrorMessage("支付宝重复调用");
        }
        //交易成功，更新数据库中的订单状态
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setPaymentTime(DateTimeUtils.strToDate(params.get("gmt_payment")));
            order.setStatus(Const.OrderStatuesEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }
//向数据库中添加支付信息
        PayInfo payInfo=new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);

        payInfoMapper.insert(payInfo);
    return ServerResponse.createBySuccess();

    }

    //查询支付状态
    public ServerResponse queryOrderPayStatus(Long orderNo,Integer userId){
        Order order=orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order==null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        if(order.getStatus()>=Const.OrderStatuesEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}
