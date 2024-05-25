package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1.创建集合用于存放从begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            // 计算开始日期后一天对应的日期加入集合
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2.创建集合用于存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询date日期对应的营业额数据（状态为“已完成”的订单金额）
            // 将data日期的时分秒补全
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // LocalTime.MIN相当于0点0分0秒
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); // LocalTime.MAX无限接近下一日的0点0分0秒
            // select sum(amount) from orders where order_time > ? and order_time < ? and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map); // 获取当天营业额
            // 处理当天营业额为0时返回null，将null转为0
            turnover = turnover == null ? 0 : turnover;
            turnoverList.add(turnover);
        }

        // 3.创建TurnoverReportVO对象封装数据返回
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
        return turnoverReportVO;
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1.创建集合用于存放从begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2.创建集合用于存放每天新增的用户数量
        List<Integer> newUserList = new ArrayList<>();
        // select count(id) from user where create_time > ? and create_time < ?
        // 3.创建集合用于存放每天总用户数量
        List<Integer> totalUserList = new ArrayList<>();
        // select count(id) from user where create_time < ?

        // 4.封装条件查询，将查询结果存放到集合中
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();

            map.put("end", endTime); // 先添加end参数 统计总用户
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime); // 再添加begin参数，统计每日新增的用户
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
        return userReportVO;
    }
}
