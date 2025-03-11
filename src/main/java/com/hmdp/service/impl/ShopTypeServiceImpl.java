package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private ShopTypeMapper shopTypeMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //用hash
    public List<ShopType> queryTypeList1() {
        // 1. 从 Redis 获取所有 Hash 数据
        Map<Object, Object> redisData = stringRedisTemplate.opsForHash().entries("cache:shopType:");
        System.out.println("redisData = " + redisData);
        List<ShopType> shopTypeList = new ArrayList<>();
        // 2. 如果 Redis 有数据，转换为 ShopType 列表
        if (!redisData.isEmpty()) {
            redisData.values().forEach(value -> {
                // 假设 Redis 中存储的是 JSON 字符串
                ShopType shopType = JSON.parseObject((String) value, ShopType.class);
                shopTypeList.add(shopType);
            });
        }

        // 3. 如果 Redis 没有数据，从数据库查询并缓存
        if (shopTypeList.isEmpty()) {
            System.out.println("从数据库查询");
            LambdaQueryWrapper<ShopType> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByAsc(ShopType::getSort);
            List<ShopType> shopTypesFromDb = shopTypeMapper.selectList(queryWrapper);
            shopTypeList.addAll(shopTypesFromDb);

            // 4. 将数据存入 Redis Hash，以 id 作为 field
            Map<String, String> cacheMap = new HashMap<>();
            for (ShopType shopType : shopTypesFromDb) {
                cacheMap.put(shopType.getId().toString(), JSON.toJSONString(shopType));
            }
            stringRedisTemplate.opsForHash().putAll("cache:shopType:", cacheMap);
            System.out.println("存入 Redis 后的数据 = " + stringRedisTemplate.opsForHash().entries("cache:shopType:"));
        }

        System.out.println("shopTypeList = " + shopTypeList);
        return shopTypeList;
    }
    //用string
    @Override
    public List<ShopType> queryTypeList() {
        String redisDara = stringRedisTemplate.opsForValue().get("cache:shopType:");
        List<ShopType> shopTypeList = JSON.parseArray(redisDara, ShopType.class);
        if (shopTypeList == null) {
            System.out.println("进入if");
            LambdaQueryWrapper<ShopType> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByAsc(ShopType::getSort);
            List<ShopType> shopTypesFromDb = shopTypeMapper.selectList(queryWrapper);
            System.out.println("shopTypesFromDb = " + shopTypesFromDb);
            List<ShopType> shopTypes = new ArrayList<>(shopTypesFromDb);
            stringRedisTemplate.opsForValue().set("cache:shopType:", JSON.toJSONString(shopTypes));
            return shopTypes;
        }
        System.out.println("shopTypeList = " + shopTypeList);
        return shopTypeList;
    }
}
