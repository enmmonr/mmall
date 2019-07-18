package com.mmall.dao;

import com.mmall.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_cart
     *
     * @mbggenerated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_cart
     *
     * @mbggenerated
     */
    int insert(Cart record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_cart
     *
     * @mbggenerated
     */
    int insertSelective(Cart record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_cart
     *
     * @mbggenerated
     */
    Cart selectByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_cart
     *
     * @mbggenerated
     */
    int updateByPrimaryKeySelective(Cart record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table mmall_cart
     *
     * @mbggenerated
     */
    int updateByPrimaryKey(Cart record);

    List<Cart> selectCartByUserId(Integer userId);

   Cart selectByUserIdAndProductId(@Param("productId") Integer productId,@Param("userId")Integer userId);

    int selectCartProductStatusByUserId(@Param("userId") Integer userId);

    int deletByUserIdAndProductIds(@Param("productIds") List<String> productIds,@Param("userId")Integer userId);

    int checkedOrUncheckedProduct(@Param("userId")Integer userId,@Param("productId") Integer productId,@Param("checked")Integer checked);
    int getCartProductCount(@Param("userId")Integer userId);

    List<Cart> selectCheckedByUserId(Integer userId);
}