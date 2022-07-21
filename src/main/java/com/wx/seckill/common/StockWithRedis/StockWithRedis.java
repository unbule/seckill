package com.wx.seckill.common.StockWithRedis;

import com.wx.seckill.common.RedisKeyDef.RedisKeyConstant;
import com.wx.seckill.common.utils.RedisPool;
import com.wx.seckill.common.utils.RedisPoolUtil;
import com.wx.seckill.entry.Stock;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Slf4j
public class StockWithRedis implements RedisKeyConstant {

    public static void updateStockWithRedis(Stock stock){
        Jedis jedis = null;

        try {
            jedis = RedisPool.getJedis();

            Transaction transaction = jedis.multi();

            RedisPoolUtil.decr(STOCK_COUNT + stock.getId());
            RedisPoolUtil.incr(STOCK_SALE + stock.getId());
            RedisPoolUtil.incr(STOCK_VERSION + stock.getId());

            List<Object> list = transaction.exec();
        }catch (Exception e){
            log.error("updateStock 获取 Jedis 实例失败：", e);
        }finally {
            RedisPool.jedisPoolClose(jedis);
        }
    }

    public static void initRedisBefore(){
        Jedis jedis = null;

        try {
            jedis = RedisPool.getJedis();

            Transaction transaction = jedis.multi();

            RedisPoolUtil.set(STOCK_COUNT + 1, "50");
            RedisPoolUtil.set(STOCK_SALE + 1, "0");
            RedisPoolUtil.set(STOCK_VERSION + 1, "0");

            List<Object> list = transaction.exec();
        }catch (Exception e){
            log.error("initRedis 获取 Jedis 实例失败：", e);
        }finally {
            RedisPool.jedisPoolClose(jedis);
        }
    }
}
