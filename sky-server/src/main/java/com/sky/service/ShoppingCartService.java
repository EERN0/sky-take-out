package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCartItem;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查看购物车
     *
     * @return
     */
    List<ShoppingCartItem> showShoppingCart();

    /**
     * 清空购物车
     */
    void cleanShoppongCart();

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    void deleteOneShoppingCartItem(ShoppingCartDTO shoppingCartDTO);
}
