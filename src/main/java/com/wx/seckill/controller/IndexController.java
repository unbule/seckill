package com.wx.seckill.controller;

import com.wx.seckill.common.limit.RedisLimit;
import com.wx.seckill.common.StockWithRedis.StockWithRedis;
import com.wx.seckill.service.api.OrderService;
import com.wx.seckill.service.api.StockService;
import com.wx.seckill.service.impl.OrderServiceImpl;
import com.wx.seckill.service.impl.StockServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@RequestMapping(value = "/")
public class IndexController {

    private static final String success = "SUCCESS";
    private static final String error = "ERROR";

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private StockServiceImpl stockService;

    /**
     * 压测前先请求该方法，初始化数据库和缓存
     */
    @RequestMapping(value = "initDBAndRedis")
    @ResponseBody
    public String initDBAndRedisBefore(HttpServletRequest request){
        int res = 0;
        try {
            res = stockService.initDBBefore();

            res &= (orderService.delOrderDBBefore() == 0 ? 1 : 0);

            StockWithRedis.initRedisBefore();
        }catch (Exception e){
            log.error("Exception: ", e);
        }
        if (res == 1){
            log.info("重置数据库和缓存成功！");
        }
        return res == 1 ? success : error;
    }

    /**
     * 秒杀基本逻辑，存在超卖问题
     *
     * @param sid
     * @return
     */
    @RequestMapping(value = "createWrongOrder", method = RequestMethod.POST)
    @ResponseBody
    public String createWrongOrder(HttpServletRequest request, int sid) {
        int res = 0;
        try {
            res = orderService.createWrongOrder(sid);
        }catch (Exception e){
            log.error("Exception: ", e);
        }
        return res == 1 ? success : error;
    }

    /**
     * 乐观锁扣库存
     * @param request
     * @param sid
     * @return
     */
    @RequestMapping(value = "createOptimisticOrder", method = RequestMethod.POST)
    @ResponseBody
    public String createOptimisticOrder(HttpServletRequest request, int sid) {
        int res = 0;
        try {
            res = orderService.createOptimisticOrder(sid);
        }catch (Exception e){
            log.error("Exception: ", e);
        }
        return res == 1 ? success : error;
    }

    /**
     * 乐观锁 + 限流
     * @param request
     * @param sid
     * @return
     */
    @RequestMapping(value = "createOptimisticLimitOrder", method = RequestMethod.POST)
    @ResponseBody
    public String createOptimisticLimitOrder(HttpServletRequest request, int sid) {
        int res = 0;
        try {
            if (RedisLimit.limit()){
                res = orderService.createOptimisticOrder(sid);
            }
        }catch (Exception e){
            log.error("Exception: ", e);
        }
        return res == 1 ? success : error;
    }

    /**
     * Redis 缓存库存，减少 DB 压力
     * @param request
     * @param sid
     * @return
     */
    @RequestMapping(value = "createOrderWithLimitAndRedis", method = RequestMethod.POST)
    @ResponseBody
    public String createOrderWithLimitAndRedis(HttpServletRequest request, int sid) {
        int res = 0;
        try {
            if (RedisLimit.limit()){
                res = orderService.createOrderWithLimitAndRedis(sid);
                if (res == 1){
                    log.info("秒杀成功");
                }
            }
        }catch (Exception e){
            log.error("Exception: ", e);
        }
        return res == 1 ? success : error;
    }

    /**
     * 限流 + Redis 缓存库存 + KafkaTest 异步下单
     * @param request
     * @param sid
     * @return
     */
    @RequestMapping(value = "createOrderWithLimitAndRedisAndKafka", method = RequestMethod.POST)
    @ResponseBody
    public String createOrderWithLimitAndRedisAndKafka(HttpServletRequest request, int sid) {
        try {
            if (RedisLimit.limit()){
                orderService.createOrderWithLimitAndRedisAndKafka(sid);
            }
        }catch (Exception e){
            log.error("Exception: ", e);
        }
        return "秒杀请求正在处理，排队中";
    }
}
