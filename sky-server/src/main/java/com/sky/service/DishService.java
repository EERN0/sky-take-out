package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品
     */
    void addDishWithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品的批量删除
     *
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param id
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     *
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO);


    /**
     * 菜品的启售停售 -- 如果是停售操作，还需要将包含当前菜品的套餐也停售
     *
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据分类id查询相关菜品
     *
     * @param categoryId
     * @return
     */
    List<Dish> list(Long categoryId);

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);
}
