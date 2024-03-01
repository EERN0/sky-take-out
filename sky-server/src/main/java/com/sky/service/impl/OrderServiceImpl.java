package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCartItem;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private AddressBookMapper addressBookMapper;

    @Resource
    private ShoppingCartMapper shoppingCartMapper;

    /**
     * 用户下单
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 1.处理业务异常(地址簿 或 购物车为空)
        // 1.1 查地址簿
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 抛出 地址簿为空 业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 1.2 查当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCartItem shoppingCartItem = ShoppingCartItem.builder().id(userId).build();
        List<ShoppingCartItem> shoppingCartItemList = shoppingCartMapper.list(shoppingCartItem);
        if (shoppingCartItemList == null || shoppingCartItemList.isEmpty()) {
            // 抛出 购物车为空 业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2.向 [订单表] 插入一条数据
        // 2.1 创建订单对象
        Orders orders = Orders.builder()
                .orderTime(LocalDateTime.now())
                .payStatus(Orders.UN_PAID)
                .status(Orders.PENDING_PAYMENT)     // 待付款
                .number(String.valueOf(System.currentTimeMillis())) // 订单号
                .phone(addressBook.getPhone())
                .consignee(addressBook.getConsignee())  // 收货人
                .userId(userId)
                .build();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);// orders对象的剩余属性从ordersSubmitDTO对象中拷贝过来
        // 2.2 插入订单数据
        orderMapper.insert(orders);


        // 3.向 [订单明细表] 批量插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 3.1 遍历购物车里的数据
        for (ShoppingCartItem cartItem : shoppingCartItemList) {
            OrderDetail orderDetail = new OrderDetail();    // 订单明细
            BeanUtils.copyProperties(cartItem, orderDetail);    // 把购物车的数据拷贝到订单明细对象中
            orderDetail.setOrderId(orders.getId());     // 设置当前订单明细关联的订单id （2.2插入订单数据时，设置了返回主键值orderId）

            orderDetailList.add(orderDetail);
        }
        // 3.2 批量插入 [订单明细表]
        orderDetailMapper.insertBatch(orderDetailList);

        // 4.下单成功后，清空购物车
        shoppingCartMapper.deleteAllByUserId(userId);

        // 5.封装返回订单数据的VO，返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId()).orderTime(orders.getOrderTime()).orderAmount(orders.getAmount())
                .build();
    }

}
