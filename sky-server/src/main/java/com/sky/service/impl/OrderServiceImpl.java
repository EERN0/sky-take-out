package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    @Resource
    private UserMapper userMapper;
    @Resource
    private WeChatPayUtil weChatPayUtil;

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
                .id(orders.getId())
                .orderNumber(orders.getNumber())    // 订单号一定要返回
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 订单支付
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // TODO 微信支付个人用户无法实现
        //// 调用微信支付接口，生成预支付交易单
        //JSONObject jsonObject = weChatPayUtil.pay(
        //        ordersPaymentDTO.getOrderNumber(), //商户订单号
        //        new BigDecimal(0.01), //支付金额，单位 元
        //        "外卖订单", // 商品描述
        //        user.getOpenid() //微信用户的openid
        //);

        // 生成空json，跳过微信支付流程
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo 订单号
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /**
     * 用户端订单分页查询
     *
     * @param pageNum  起始索引 = (查询页码-1) * 每页显示记录数
     * @param pageSize 每页显示记录数
     * @param status   订单状态: 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();  // 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id 订单id
     */
    @Override
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询订单的地址信息
        AddressBook addressBook = addressBookMapper.getById(orders.getAddressBookId());
        String address = addressBook.getProvinceName() + addressBook.getDistrictName() + addressBook.getDistrictName() + addressBook.getDetail();
        orders.setAddress(address);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);  // 把order对象的属性值拷贝给orderVO对象
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id 订单id
     */
    public void userCancelById(Long id) throws Exception {
        // 1.根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 2.校验订单是否存在
        if (ordersDB == null) {
            // 抛出 订单不存在 业务异常
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            // 抛出 订单状态错误 异常 （此时是用户取消订单，订单能是 待付款 或 待接单）
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 3.订单处于 待接单状态 下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {

            // TODO 无法实现：调用微信支付退款接口
            //weChatPayUtil.refund(
            //        ordersDB.getNumber(), // 商户订单号
            //        ordersDB.getNumber(), // 商户退款单号
            //        new BigDecimal(0.01),// 退款金额，单位 元
            //        new BigDecimal(0.01));// 原订单金额

            // 直接将 订单的支付状态修改为 已退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id 订单id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCartItem> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCartItem shoppingCart = new ShoppingCartItem();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }
}
