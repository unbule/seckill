package com.wx.seckill.common;

import com.wx.seckill.common.RedisKeyDef.RedisKeyConstant;
import com.wx.seckill.common.utils.RedisPoolUtil;
import com.wx.seckill.entry.Stock;
import com.wx.seckill.service.impl.StockServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RedisPreheatRunner implements ApplicationRunner, RedisKeyConstant {

    @Autowired
    private StockServiceImpl stockService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Stock stock = stockService.getStockById(1);

        // 删除旧缓存
        RedisPoolUtil.del(STOCK_COUNT + stock.getCount());
        RedisPoolUtil.del(STOCK_SALE + stock.getSale());
        RedisPoolUtil.del(STOCK_VERSION + stock.getVersion());
        // 缓存预热
        int sid = stock.getId();
        RedisPoolUtil.set(STOCK_COUNT + sid, String.valueOf(stock.getCount()));
        RedisPoolUtil.set(STOCK_SALE + sid, String.valueOf(stock.getSale()));
        RedisPoolUtil.set(STOCK_VERSION + sid, String.valueOf(stock.getVersion()));
    }
}
