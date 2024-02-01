package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCartItem;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断当前加入到购物车中的商品是否已经存在了
        ShoppingCartItem shoppingCartItem = new ShoppingCartItem();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCartItem);
        Long userId = BaseContext.getCurrentId();   // 使用ThreadLocal获取当前用户微信id
        shoppingCartItem.setUserId(userId);

        // select * from shopping_cart WHERE user_id = ? and dish_id = ? and dish_flavor = ? 或者 select * from shopping_cart WHERE user_id = ? and dish_id = ?
        // 或者 select * from shopping_cart WHERE user_id = ? and setmealId = ?
        // 动态sql，字段非空就充当条件，加上and一起去数据库里面查
        List<ShoppingCartItem> list = shoppingCartMapper.list(shoppingCartItem);    // 若购物车数据库表有这条数据，肯定也只有一条。写成List是为了方便后续的【查看购物车】操作

        // 如果已经存在了，只用将数量+1（若添加的是不同口味的菜品，购物车还得添加一条数据）
        if (list != null && !list.isEmpty()) {
            ShoppingCartItem cart = list.get(0);    // list非空，肯定只有一条数据
            cart.setNumber(cart.getNumber() + 1);   // update shopping_cart set number = ? where id = ?
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 如果不存在，需要插入一条购物车数据

            // 判断本次添加到购物车的是菜品 or 套餐（shoppingCartDTO中dishId和setmeaId有且只有一个）
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {    // 菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCartItem.setName(dish.getName());
                shoppingCartItem.setImage(dish.getImage());
                shoppingCartItem.setAmount(dish.getPrice());
            } else {    // dishId==null，是套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCartItem.setName(setmeal.getName());
                shoppingCartItem.setImage(setmeal.getImage());
                shoppingCartItem.setAmount(setmeal.getPrice());
            }
            shoppingCartItem.setNumber(1);  // 商品数量设置为1
            shoppingCartItem.setCreateTime(LocalDateTime.now());

            shoppingCartMapper.insert(shoppingCartItem);
        }
    }

    /**
     * 查看购物车
     *
     * @return
     */
    @Override
    public List<ShoppingCartItem> showShoppingCart() {
        // 获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCartItem shoppingCartItem = ShoppingCartItem.builder()
                .userId(userId)
                .build();

        List<ShoppingCartItem> list = shoppingCartMapper.list(shoppingCartItem);
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppongCart() {
        // 获取当前微信用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteAllByUserId(userId);
    }

    /**
     * 删除购物车中一个商品
     *
     * @param shoppingCartDTO
     */
    @Override
    public void deleteOneShoppingCartItem(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCartItem shoppingCartItem = new ShoppingCartItem();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCartItem);
        Long userId = BaseContext.getCurrentId();
        shoppingCartItem.setUserId(userId);

        // 动态查询购物车数据，必须加上用户id！(一个用户一个购物车)
        List<ShoppingCartItem> list = shoppingCartMapper.list(shoppingCartItem);
        if (list != null && !list.isEmpty()) {
            ShoppingCartItem cartItem = list.get(0);
            // 删除购物车中该条商品数据
            Integer number = cartItem.getNumber() - 1;
            if (number > 0) {
                cartItem.setNumber(number);
                shoppingCartMapper.updateNumberById(cartItem);
            } else {    // number变为0后，直接干掉购物车数据库里的这条数据
                shoppingCartMapper.deleteByShoppingCartItemId(cartItem);
            }
        }
    }
}
