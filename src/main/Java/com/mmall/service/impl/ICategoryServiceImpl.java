package com.mmall.service.impl;

import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service("iCategoryService")
public class ICategoryServiceImpl implements ICategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    //声明日志
    private Logger logger=LoggerFactory.getLogger(ICategoryServiceImpl.class);

    //增加分类
    public ServerResponse<Category> addCategory(Integer parentId,String categoryName){
        if(parentId==null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("添加品类参数无效");
        }
        Category category=new Category();
        category.setParentId(parentId);
        category.setName(categoryName);
        category.setStatus(true);
        int rowCount=categoryMapper.insert(category);
        if(rowCount>0)
        {
            return ServerResponse.createBySuccessMessage("增加品类成功");
        }else {
            return ServerResponse.createByErrorMessage("增加品类失败");
        }
    }
    //修改分类名
    @Override
    public ServerResponse setCategoryName(Integer categoryId, String categoryName) {
        if (categoryId==null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("参数无效");
        }
        Category category=new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        int rowCount=categoryMapper.updateByPrimaryKeySelective(category);
        if(rowCount>0){
            return ServerResponse.createBySuccessMessage("更新品类名成功");
        }
        return ServerResponse.createByErrorMessage("更新品类名失败");
    }

    //获取平级分类节点(不应该只有一个分类结果，所以用集合存放分类)
    public ServerResponse<List<Category>> getCategory(Integer categoryId){

        List<Category> categoryList=categoryMapper.getCategory(categoryId);
        if(CollectionUtils.isEmpty(categoryList))
        {
            logger.info("未找到该分类的子分类");
        }
        return  ServerResponse.createBySuccess(categoryList);
    }

   // 递归查询本节点的id及孩子节点的id
    @Override
    public ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(Integer categoryId) {
        //查找节点，并使用Set集合排重
        Set<Category> categorySet=new HashSet<>();
        findChildren(categorySet,categoryId);

        //获取分类的id
        List<Integer> list=new ArrayList<>();
        //遍历set集合中的分类信息，将分类id放入list集合
        for (Category category:categorySet
             ) {
            list.add(category.getId());
        }
        return ServerResponse.createBySuccess(list);
    }
    //递归算法
    private Set<Category> findChildren( Set<Category> categorySet,Integer categoryId ){
        //查找该分类信息，如果有则加入set集合中
        Category category=categoryMapper.selectByPrimaryKey(categoryId);
        if(category!=null){
            categorySet.add(category);
        }
        //根据该分类信息，查找是否有以其id为父节点的子分类
        List<Category> categoryList=categoryMapper.getCategory(category.getId());
        //遍历子节点，并递归查找是否仍有子节点
        for (Category ca:categoryList
             ) {
            findChildren(categorySet,ca.getId());
        }
        return categorySet;

    }
}
