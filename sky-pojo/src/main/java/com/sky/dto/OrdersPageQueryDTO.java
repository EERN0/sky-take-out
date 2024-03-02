package com.sky.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单分页查询DTO
 */
@Data
public class OrdersPageQueryDTO implements Serializable {
    // 起始页码索引
    private int page;

    // 每页显示记录数
    private int pageSize;

    // 订单号
    private String number;

    // 手机号
    private String phone;

    // 订单状态: 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long userId;

}
