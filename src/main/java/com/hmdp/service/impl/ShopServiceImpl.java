package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private ShopMapper shopMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopTypeServiceImpl shopTypeServiceImpl;

    @Override
    public Result queryById(Long id) {
        //1.从Redis查缓存
        String shopJson = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
        //2.是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //3.存在直接返回
        Shop shop = getById(id);
        //4.不存在 访问数据库 如果不存在返回fail 存在存入数据库
        if (shop == null) {
            //缓存加入空值 防止缓存穿透
            stringRedisTemplate.opsForValue().set("cache:shop:" + id, "",3, TimeUnit.MINUTES);
            return Result.fail("不存在");
        }
        //超时剔除
        stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop),30, TimeUnit.DAYS);
        //5.返回 and 加入redis缓存
        return Result.ok(shop);
    }
    @Transactional(rollbackFor = Exception.class) //不能回滚redis
    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id为空");
        }
        //更新数据库  -》  删除缓存
        updateById(shop);
        stringRedisTemplate.delete("cache:shop:" + shop.getId());
        return Result.ok();
    }
}
