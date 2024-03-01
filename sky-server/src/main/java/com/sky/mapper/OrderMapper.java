package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单表 mapper
 */
@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     */
    void insert(Orders orders);
}
