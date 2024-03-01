package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端订单分页查询
     *
     * @param page     起始索引 = (查询页码-1) * 每页显示记录数
     * @param pageSize 每页显示记录数
     * @param status   订单状态: 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);
}
