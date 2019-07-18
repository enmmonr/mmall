package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.vo.ProductDetailVo;

public interface IProductService {
    //添加或更新产品信息
    ServerResponse addOrUpdateProduct(Product product);
    //产品上下架
    ServerResponse<String> setSaleStatus(Integer productId,Integer status);
    //产品详情
    ServerResponse<ProductDetailVo> manageProductDetail(Integer productId);
    //分页获取产品集合
    ServerResponse getProductList(Integer pageNum, Integer pageSize);
    //搜索产品
    ServerResponse manageSearchProduct(Integer pageNum,Integer pageSize,Integer productId,String productName);
    //前台产品详情
    ServerResponse<ProductDetailVo> productDetail(Integer productId);
    //前台产品搜索及动态排序
    ServerResponse<PageInfo> searchProduct(String keyword, Integer categoryId, Integer pageNum, Integer pageSize, String orderBy);
}
