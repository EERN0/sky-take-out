package com.sky.mapper;

import com.sky.entity.ShoppingCartItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态条件查询
     *
     * @param shoppingCartItem
     * @return
     */
    List<ShoppingCartItem> list(ShoppingCartItem shoppingCartItem);


    /**
     * 根据id修改购物车中商品数量
     *
     * @param shoppingCartItem
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCartItem shoppingCartItem);

    /**
     * 插入购物车数据
     *
     * @param shoppingCartItem
     */
    @Insert("insert into shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) " +
            "values (#{name},#{image},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{createTime})")
    void insert(ShoppingCartItem shoppingCartItem);

    /**
     * 根据用户id删除购物车数据
     *
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteAllByUserId(Long userId);

    /**
     * 根据购物车商品id删除对应的数据
     * @param shoppingCartItem
     */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteByShoppingCartItemId(ShoppingCartItem shoppingCartItem);

    /**
     * 批量插入购物车数据
     *
     * @param shoppingCartList 购物车数据列表
     */
    void insertBatch(List<ShoppingCartItem> shoppingCartList);
}
