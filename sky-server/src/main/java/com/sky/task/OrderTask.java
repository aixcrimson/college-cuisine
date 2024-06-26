package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时处理超时未支付订单的方法
     */
    // @Scheduled(cron = "0 * * * * ?") // 每分钟的第 0 秒触发一次
    public void processTimeoutOrder(){
        log.info("定时处理超时未支付订单:{}", LocalDateTime.now());

        // 处理下单后超过15分钟未支付的订单，每分钟触发一次
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15); // 当前时间 - 15分钟
        // select * from orders where status = ? and order_time < (当前时间 - 15分钟)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        // 设置订单状态（已取消），取消原因，取消时间
        if(ordersList !=null && ordersList.size()>0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 定时处理处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨一点触发一次
    // @Scheduled(cron = "0/5 * * * * ? ")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单:{}", LocalDateTime.now());

        // 处理凌晨0点后下单的订单，每天凌晨一点触发一次
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if(ordersList !=null && ordersList.size()>0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
