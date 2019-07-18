package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.vo.OrderVo;

import java.util.Map;

public interface IOrderService {
    ServerResponse pay(Integer userId,Long orderNo,String path);
    ServerResponse aliCallback(Map<String,String> params);

    ServerResponse queryOrderPayStatus(Long orderNo,Integer userId);
    ServerResponse create(Integer userId,Integer shippingId);
    ServerResponse getOrderCartProduct(Integer userId);
    ServerResponse list(Integer pageNum,Integer pageSize,Integer userId);
    ServerResponse<OrderVo> detail(Integer userId, Long orderNo);
    ServerResponse cancel(Integer userId,Long orderNo);

    ServerResponse<PageInfo> manageList(Integer pageNum, Integer pageSize);
    ServerResponse<OrderVo> manageDetail(Long orderNo);
    ServerResponse<String> sendGoods(Long orderNo);
    ServerResponse<PageInfo> search(Long order,Integer pageNum,Integer pageSize);
}
