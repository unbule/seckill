package com.wx.seckill.common.limit;

import com.wx.seckill.common.utils.RedisPool;
import com.wx.seckill.common.utils.ScriptUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Collections;

@Slf4j
public class RedisLimit {

    private static final int FAIL_CODE = 0;

    private static Integer limit = 5;

    public static Boolean limit(){
        Jedis jedis = null;
        Object result = null;

        try {
            jedis = RedisPool.getJedis();

            String script = ScriptUtil.getScript("limit.lua");

            String key = String.valueOf(System.currentTimeMillis() / 1000);

            result = jedis.eval(script, Collections.singletonList(key), Collections.singletonList(String.valueOf(limit)));
            if (FAIL_CODE != (Long) result){
                log.info("成功获取令牌");
                return true;
            }
        }catch (Exception e){
            log.error("limit 获取 Jedis 实例失败：", e);
        }finally {
            RedisPool.jedisPoolClose(jedis);
        }
        return false;
    }
}
