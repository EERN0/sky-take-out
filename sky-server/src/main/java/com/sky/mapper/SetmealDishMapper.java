package com.sky.mapper;


import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 套餐-菜品 对应的表 （多对多）
 */
@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询对应的套餐id
     *
     * @return
     */
    // select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 保存套餐和菜品的关系
     *
     * @param setmealDishes
     */
    //@AutoFill(OperationType.INSERT)   // 注意这里第1个参数不是实体对象而是SetmealDish集合，不能用@AutoFill自动填充
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id集合，删除seat_meal表的数据
     *
     * @param setmealIds
     */
    void deleteBatch(List<Long> setmealIds);

    /**
     * 根据套餐id，删除setmeal_dish表的数据
     *
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteById(Long setmealId);

    /**
     * 根据套餐id查询对应的套餐-菜品数据
     *
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getDishListBySetmealId(Long setmealId);

}
