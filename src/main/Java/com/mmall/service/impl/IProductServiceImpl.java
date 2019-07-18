package com.mmall.service.impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.ICategoryService;
import com.mmall.service.IProductService;
import com.mmall.utils.DateTimeUtils;
import com.mmall.utils.PropertiesUtil;
import com.mmall.vo.ProductDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("iProductService")
public class IProductServiceImpl implements IProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ICategoryService iCategoryService;

    //添加或更新产品信息
    public ServerResponse addOrUpdateProduct(Product product){
        if (product!=null){
            //将主图的第一张设为子图
            if(StringUtils.isNotBlank(product.getSubImages())){
                String[] subImages=product.getSubImages().split(",");
                if(subImages.length>0) {
                    product.setMainImage(subImages[0]);
                }
            }
            //如果id不存在则向数据库中增加一条记录
            if(product.getId()==null){
                int rowCount=productMapper.insert(product);
                if(rowCount>0){
                    return ServerResponse.createBySuccess("添加产品信息成功");
                }else{
                    return  ServerResponse.createByErrorMessage("添加产品信息失败");
                }
            }else {
                //如果id存在则更新产品信息
                int rowCount = productMapper.updateByPrimaryKey(product);
                if (rowCount > 0) {
                    return ServerResponse.createBySuccess("更新产品信息成功");
                } else {
                    return ServerResponse.createByErrorMessage("更新产品信息失败");
                }
            }
        }

        return ServerResponse.createByErrorMessage("产品更新或保存的参数不正确");
    }

    //产品上下架
    public ServerResponse<String> setSaleStatus(Integer productId,Integer status){
        if(productId == null || status == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product=new Product();
        product.setId(productId);
        product.setStatus(status);
       int rowCount= productMapper.updateByPrimaryKeySelective(product);
        if (rowCount>0){
            return ServerResponse.createBySuccessMessage("产品状态更新成功");
        }
        return ServerResponse.createByErrorMessage("产品状态更新失败");

    }

    //产品详情
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId){
        if(productId==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product=productMapper.selectByPrimaryKey(productId);
        if(product==null){
            return ServerResponse.createByErrorMessage("商品已下架或删除");
        }
        ProductDetailVo productDetailVo=assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);

    }


    public ServerResponse<PageInfo> getProductList(Integer pageNum, Integer pageSize){
        //初始化页面大小
        PageHelper.startPage(pageNum,pageSize);
        //从数据库中查找商品
        List<Product> list=productMapper.getProductList();

        //排除不必要的数据
        List<ProductDetailVo> productDetailVos=new ArrayList<>();
        for (Product product:list
             ) {
            productDetailVos.add(assembleProductDetailVo(product));
        }
        //获取分页大小
        PageInfo pageInfo=new PageInfo(list);
        pageInfo.setList(productDetailVos);
        return ServerResponse.createBySuccess(pageInfo);

    }

    //产品搜索
    public ServerResponse<PageInfo> manageSearchProduct(Integer pageNum,Integer pageSize,Integer productId,String productName){
        PageHelper.startPage(pageNum,pageSize);
        if(StringUtils.isNotBlank(productName)){
            productName=new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<Product> productList=productMapper.selectListByIdAndName(productId,productName);

        List<ProductDetailVo> productDetailVoList=new ArrayList<>();
        for (Product product:productList
             ) {
            productDetailVoList.add(assembleProductDetailVo(product));
        }
        PageInfo pageInfo=new PageInfo(productList);
        pageInfo.setList(productDetailVoList);
        return  ServerResponse.createBySuccess(pageInfo);

    }

    private ProductDetailVo assembleProductDetailVo(Product product){
        ProductDetailVo productDetailVo=new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.ip","http://img.happymmall.com/"));

        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null){
            productDetailVo.setParentCategoryId(0);//默认根节点
        }else{
            productDetailVo.setParentCategoryId(category.getParentId());
        }
        productDetailVo.setCreateTime(DateTimeUtils.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtils.dateToStr(product.getUpdateTime()));

        return productDetailVo;

    }

    //前台产品详情
    public ServerResponse<ProductDetailVo> productDetail(Integer productId){
        if(productId==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product=productMapper.selectByPrimaryKey(productId);
        if(product==null){
            return ServerResponse.createByErrorMessage("商品已下架或删除");
        }
        if(product.getStatus()!= Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServerResponse.createByErrorMessage("商品已下架或删除");
        }
        ProductDetailVo productDetailVo=assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);

    }

    //前台产品搜索及动态排序
    public ServerResponse<PageInfo> searchProduct(String keyword,Integer categoryId,Integer pageNum,Integer pageSize,String orderBy){
        //判断关键字与分类iD是否错误
        if(StringUtils.isBlank(keyword) && categoryId==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //用于存储分类id集合
        List<Integer> integerList=new ArrayList<>();
        //查找分类
        if(categoryId!=null){
            Category category=categoryMapper.selectByPrimaryKey(categoryId);
            if(category==null && StringUtils.isBlank(keyword)){
                //数据未命中，逻辑上不是错误，返回一个空值
                PageHelper.startPage(pageNum,pageSize);
                List<ProductDetailVo> list= Lists.newArrayList();
                PageInfo pageInfo=new PageInfo(list);
                return ServerResponse.createBySuccess(pageInfo);
            }
            //获取categoryId的所有子分类id
            integerList=iCategoryService.getCategoryAndDeepChildrenCategory(category.getId()).getData();
        }
        //关键字拼接
        if(StringUtils.isNotBlank(keyword)) {

            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        PageHelper.startPage(pageNum,pageSize);
        //动态排序
        if(StringUtils.isNotBlank(orderBy)){
            if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)) {
                String[] orderByArg = orderBy.split("_");
                PageHelper.orderBy(orderByArg[0] + " " + orderByArg[1]);
            }
        }
        List<Product> productList=productMapper.selectListByNameAndCategoryIds(integerList.size()==0?null:integerList,StringUtils.isBlank(keyword)?null:keyword);
        List<ProductDetailVo> productDetailVos=Lists.newArrayList();
        for (Product p:productList
             ) {
            ProductDetailVo productListVo = assembleProductDetailVo(p);
            productDetailVos.add(productListVo);
        }
        PageInfo pageInfo=new PageInfo(productList);
        pageInfo.setList(productDetailVos);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
