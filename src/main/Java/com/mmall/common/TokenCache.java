package com.mmall.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//本地缓存
public class TokenCache {
    //声明日志
    private static  Logger logger= LoggerFactory.getLogger(TokenCache.class);
    //token名前缀
    public static final String TOKEN_PREFIX = "token_";

    /*
     * 创建本地缓存块
     * 采用调用链模式
     * initialCapacity(1000)   设置缓存初始化容量
     * maximumSize(10000)   设置缓存最大容量，当超过这个容量时，缓存会调用LRU（最少缓存）算法来移除缓存项
     * expireAfterAccess(12, TimeUnit.HOURS) 设置缓存有效期 此方法表示设置写入对象多长时间未被访问后过期
     * expireAfterWrite方法指定对象被写入到缓存后多久过期*/
    private static LoadingCache<String,String> localCache= CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(10000).
            expireAfterAccess(12, TimeUnit.HOURS).build(new CacheLoader<String, String>() {
                //默认数据加载实现，当调用get取值时，如果key没有对应的值，就调用这个方法进行加载
        @Override
        public String load(String s) throws Exception {
            return "null";
        }
    });
    public static void setKey(String key,String value){
        localCache.put(key,value);
    }
    public static String getKey(String key)
    {
        String value=null;
        try {
            value=localCache.get(key);
            if ("null".equals(value))
            {
             return null;
            }
            return value;
        } catch (ExecutionException e) {
            logger.error("localCache get error",e);
        }
        return null;
    }
}
