package com.wx.seckill.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wx.seckill.common.RedisKeyDef.RedisKeyConstant;
import com.wx.seckill.common.StockWithRedis.StockWithRedis;
import com.wx.seckill.common.utils.RedisPoolUtil;
import com.wx.seckill.entry.Stock;
import com.wx.seckill.entry.StockOrder;
import com.wx.seckill.dao.StockOrderMapper;
import com.wx.seckill.service.api.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class OrderServiceImpl implements OrderService, RedisKeyConstant {

    @Autowired
    private StockServiceImpl stockServiceImpl;

    @Autowired
    private StockOrderMapper stockOrderMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.template.default-topic}")
    private String kafkaTopic;

    private Gson gson = new GsonBuilder().create();

    @Override
    public int delOrderDBBefore() {
        return stockOrderMapper.delOrderDBBefore();
    }

    @Override
    public int createWrongOrder(int sid) throws Exception {
        Stock stock = checkStock(sid);
        saleStock(stock);
        int res = createOrder(stock);
        return res;
    }

    @Override
    public int createOptimisticOrder(int sid) throws Exception {
        Stock stock = checkStock(sid);
        saleStockOptimstic(stock);
        int id = createOrder(stock);
        return id;
    }

    @Override
    public int createOrderWithLimitAndRedis(int sid) throws Exception {
        Stock stock = checkStockWithRedis(sid);

        saleStockOptimsticWithRedis(stock);

        int res = createOrder(stock);

        return res;
    }

    @Override
    public void createOrderWithLimitAndRedisAndKafka(int sid) throws Exception {
        Stock stock = checkStockWithRedis(sid);

        kafkaTemplate.send(kafkaTopic, gson.toJson(stock));

        log.info("消息发送至 Kafka 成功");
    }

    @Override
    public int consumerTopicToCreateOrderWithKafka(Stock stock) throws Exception {
        saleStockOptimsticWithRedis(stock);
        int res = createOrder(stock);
        if (res == 1){
            log.info("Kafka 消费 Topic 创建订单成功");
        }else {
            log.info("Kafka 消费 Topic 创建订单失败");
        }
        return res;
    }

    private int createOrder(Stock stock) {
        StockOrder stockOrder = new StockOrder();
        stockOrder.setSid(stock.getId());
        stockOrder.setName(stock.getName());
        stockOrder.setCreateTime(new Date());
        int res = stockOrderMapper.insertSelective(stockOrder);
        if (res == 0){
            throw new RuntimeException("创建订单失败");
        }
        return res;
    }

    private int saleStock(Stock stock) {
        stock.setSale(stock.getSale()+1);
        stock.setCount(stock.getCount()-1);
        return stockServiceImpl.updateStockById(stock);
    }

    private void saleStockOptimstic(Stock stock) throws Exception{
        int count = stockServiceImpl.updateStockByOptimistic(stock);
        if (count == 0){
            throw new RuntimeException("并发更新库存失败");
        }
    }

    private Stock checkStock(int sid) throws Exception {
        Stock stock = stockServiceImpl.getStockById(sid);
        if (stock.getCount()<1){
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    private Stock checkStockWithRedis(int sid) throws Exception{
        Integer count = Integer.parseInt(RedisPoolUtil.get(STOCK_COUNT + sid));
        Integer sale = Integer.parseInt(RedisPoolUtil.get(STOCK_SALE + sid));
        Integer version = Integer.parseInt(RedisPoolUtil.get(STOCK_VERSION + sid));

        if (count < 1){
            log.info("库存不足");
            throw new RuntimeException("库存不足 Redis currentCount: " + sale);
        }
        Stock stock = new Stock();
        stock.setId(sid);
        stock.setCount(count);
        stock.setSale(sale);
        stock.setVersion(version);

        stock.setName("手机");

        return stock;
    }

    private void saleStockOptimsticWithRedis(Stock stock) throws Exception {
        int res = stockServiceImpl.updateStockByOptimistic(stock);
        if (res == 0) {
            throw new RuntimeException("并发更新库存失败");
        }
        // 更新 Redis
        StockWithRedis.updateStockWithRedis(stock);
    }
}
