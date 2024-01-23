package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     *
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 新增套餐
     * insert into setmeal
     *
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据套餐id查询套餐数据
     *
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal where id = #{setmealId}")
    Setmeal getById(Long setmealId);

    /**
     * 根据套餐id集合批量删除套餐
     *
     * @param setmealIds
     */
    void deleteBatch(List<Long> setmealIds);

    /**
     * 根据套餐id动态更新套餐表的数据
     *
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    // 自动给实体对象的updateTime、updateUser等赋值，参数第1个只能是实体类
    void update(Setmeal setmeal);

    /**
     * 动态条件查询套餐
     *
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据套餐id查询菜品选项
     *
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);
}
