package com.mmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.utils.BigDecimalUtil;
import com.mmall.utils.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;

@Service("iCartService")
public class ICartServiceImpl implements ICartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    //向购物车添加商品
    public ServerResponse<CartVo> add(Integer userId,Integer productId,Integer count){
        //判断参数是否有效
        if(productId==null || count==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc()) ;
        }
        Cart cartItem=cartMapper.selectByUserIdAndProductId(productId,userId);
        if(cartItem==null){
            Cart cart=new Cart();
            cart.setProductId(productId);
            cart.setUserId(userId);
            cart.setQuantity(count);
            cart.setChecked(Const.Cart.CHECKED);
            cartMapper.insert(cart);
        }else{
            count=cartItem.getQuantity()+count;
            cartItem.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cartItem);
        }

        return  this.list(userId);
    }
    //获取购物车列表
    public ServerResponse<CartVo> list (Integer userId){
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    //更新购物车某个产品数量
    public ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count){
        if(productId==null || count==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart=cartMapper.selectByUserIdAndProductId(productId,userId);
        if(cart!=null){
            cart.setQuantity(count);
        }
        //更新数据库
        cartMapper.updateByPrimaryKey(cart);
        return this.list(userId);

    }

    //删除购物车中的某个产品
    public ServerResponse<CartVo> deleteCart(Integer userId,String productIds){
        List<String> productList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cartMapper.deletByUserIdAndProductIds(productList,userId);
        return this.list(userId);

    }

    //购物车选中某个商品
    public ServerResponse<CartVo> selectOrUnSelect(Integer userId,Integer productId,Integer checked){
      cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }
    //查询在购物车里的产品数量
     public ServerResponse<Integer> getCartProductCount(Integer userId){
         if(userId == null){
             return ServerResponse.createBySuccess(0);
         }
       int count= cartMapper.getCartProductCount(userId);
         return ServerResponse.createBySuccess(count);
     }

    private CartVo getCartVoLimit(Integer userId){
        CartVo cartVo=new CartVo();
        //Cart表中一条记录一个userID代表一项
        List<Cart> carts=cartMapper.selectCartByUserId(userId);//查询当前用户购物车中所有的商品项
        List<CartProductVo> cartProductVoList= Lists.newArrayList();
        System.out.println(carts.size());
        BigDecimal cartTotalPrice = new BigDecimal("0");//购物车最终总价计算
        if (CollectionUtils.isNotEmpty(carts)){
            for (Cart cart:carts) {
                CartProductVo cartProductVo=new CartProductVo();
                cartProductVo.setId(cart.getId());
                cartProductVo.setProductId(cart.getProductId());
                cartProductVo.setUserId(cart.getUserId());
                System.out.println(cart);
                System.out.println(cart.getUserId());
                Product product = productMapper.selectByPrimaryKey(cart.getProductId());
                if (product != null) {
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductStaus(product.getStatus());
                    cartProductVo.setProductSubTitle(product.getSubtitle());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());
                    //判断库存
                    int buyLimitCount = 0;
                    if (product.getStock() >= cart.getQuantity()) {
                        //库存充足的时候
                        buyLimitCount = cart.getQuantity();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    } else {
                        //库存不足强制设置为最大库存
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        //更新购物车中的购买数
                        Cart cartforQuantity=new Cart();
                        cartforQuantity.setId(cart.getId());
                        cartforQuantity.setQuantity(buyLimitCount);
                        cartMapper.updateByPrimaryKeySelective(cartforQuantity);//只更新库存信息
                    }
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算总价，此处计算的是单个商品与库存的总价
                    cartProductVo.setProducTotalPrice(BigDecimalUtil.mul(cartProductVo.getQuantity(),product.getPrice().doubleValue()));
                    cartProductVo.setProductChecked(cart.getChecked());
                    if (cart.getChecked()==Const.Cart.CHECKED){
                        //如果是选中状态，则算入最终的购物车总价中
                        cartTotalPrice=BigDecimalUtil.add(cartProductVo.getProductTotalPrice().doubleValue(),cartTotalPrice.doubleValue());

                    }
                    cartProductVoList.add(cartProductVo);

                }
                cartVo.setCartTotalPrice(cartTotalPrice);
                cartVo.setCartProductVoList(cartProductVoList);
                cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
                cartVo.setAllChecked(this.getAllCheckedStatus(userId));
            }
        }
        return cartVo;
    }

    //判断购物车中所有商品是否全选
    private boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductStatusByUserId(userId)==0;
    }
}
