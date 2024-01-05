package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增 菜品 和 对应的口味
     * 涉及到两张表的操作，要加上事务注解 @Transactional （要么全成功，要么全失败）
     * 并在启动类中开启注解方式的事务管理 @EnableTransactionManagement（有争议，好像默认就开启了?）
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void addDishWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表插入1条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值 (注意DishMapper.xml中insert语句的写法)
        Long dishId = dish.getId();

        // 向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBench(flavors);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 泛型是DishVO，不是Dish类 (分页要展示 菜品分类名称，Dish类中没有这个成员)
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品的批量删除
     * <p>
     * 涉及多个表的操作，加事务注解，保证数据的一致性
     * </p>
     *
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否在启售中（启售的不能删除）
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                // 菜品在启售中,不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断当前菜品是否关联了套餐（关联了套餐后就不能删除）
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && !setmealIds.isEmpty()) {
            // 当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表中的菜品数据
        for (Long id : ids) {
            dishMapper.deleteById(id);
            // 删除菜品关联的口味数据
            dishFlavorMapper.deleteById(id);
        }
    }

}
