package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 订单明细表 mapper
 */
@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入n条订单明细
     */
    void insertBatch(List<OrderDetail> orderDetailList);
}
