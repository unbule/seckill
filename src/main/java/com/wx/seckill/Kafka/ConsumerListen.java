package com.wx.seckill.Kafka;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wx.seckill.entry.Stock;
import com.wx.seckill.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ConsumerListen {

    private Gson gson = new GsonBuilder().create();

    @Autowired
    private OrderServiceImpl orderService;

    @KafkaListener(topics = "SECONDS-KILL-TOPIC")
    public void listen(ConsumerRecord<String, String> record) throws Exception{
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());

        String message = (String) kafkaMessage.get();

        Stock stock = gson.fromJson((String) message, Stock.class);

        orderService.consumerTopicToCreateOrderWithKafka(stock);
    }
}
