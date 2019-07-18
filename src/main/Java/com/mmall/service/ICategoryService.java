package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.Category;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ICategoryService {
    //增加分类
    ServerResponse addCategory(Integer parentId, String categoryName);

    //修改分类名
    ServerResponse setCategoryName(Integer categoryId,String categoryName);

    //获取分类信息
    ServerResponse<List<Category>> getCategory(Integer categoryId);
    //递归查询本节点的id及孩子节点的id
    ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(Integer categoryId);
}
