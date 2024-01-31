package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import com.sky.vo.DishItemVO;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 套餐业务实现
 */
@Builder    // 加@Builder注解，可以不用set方法。直接 类名.builder().成员变量()
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的对应关系
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void addSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 1.向套餐表插入套餐数据
        setmealMapper.insert(setmeal);
        // 2.获取生成的套餐id
        Long setmealId = setmeal.getId();

        // 3.绑定 SetmealDish实体对象的dishId和setmealId
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            // TODO setmealDish的dishId是怎么拿到的？---> 通过前端选择的数据拿到dishId，与套餐id绑定后，写入数据库
            System.out.println(setmealDish.getName() + ": " + setmealDish.getDishId());
            setmealDish.setSetmealId(setmealId);
        }

        // 4.保存套餐和菜品s的关系到套餐-菜品表
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        // 给前端返回SetmealVO实体对象展示
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐的批量删除
     * <p>
     * 启售中的套餐不能删，需要删除 setmeal表和setmeal_dish表中的setmeal数据
     * </p>
     *
     * @param setmealIds
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> setmealIds) {
        // 查套餐是否启售
        for (Long setmealId : setmealIds) {
            Setmeal setmeal = setmealMapper.getById(setmealId);
            if (setmeal.getStatus() == StatusConstant.ENABLE) { // 套餐启售，不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 删除套餐表中数据
        setmealMapper.deleteBatch(setmealIds);

        // 删除套餐-菜品关联表中的数据
        setmealDishMapper.deleteBatch(setmealIds);
    }

    /**
     * 根据id查询套餐和对应的菜品
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        List<SetmealDish> list = setmealDishMapper.getDishListBySetmealId(setmeal.getId());
        setmealVO.setSetmealDishes(list);

        return setmealVO;
    }

    /**
     * 修改套餐：修改套餐表 + 套餐-菜品 表
     *
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void update(SetmealDTO setmealDTO) {
        // 1.修改setmeal表（有修改时间、修改用户，所以要用到Setmeal类）
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        setmealMapper.update(setmeal);

        // 2.修改套餐对应的菜品，操作setmeal_dish表。
        // 先删除原表的数据，再把前端传来的套餐对应菜品数据写入到表中
        setmealDishMapper.deleteById(setmealDTO.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes(); // 前端获取到的菜品列表数据 (前端添加菜品时，没有传进来setmealId，得自己加上)
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealDTO.getId()));     // 给setmealDish加上对应的套餐id
            setmealDishMapper.insertBatch(setmealDishes);   // 批量插入套餐-菜品数据
        }
    }

    /**
     * 套餐的启售、停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {  // id是套餐id
        // 根据套餐id查询对应的数据。启售的套餐可以停售，停售的套餐可以启售(启售的套餐中，菜品不能有停售的)
        Setmeal setmeal = setmealMapper.getById(id);

        if (setmeal.getStatus() == StatusConstant.ENABLE) {   // 套餐在启售
            if (status == StatusConstant.DISABLE) {           // 要停售套餐
                // TODO: 为什么只要传status、id就行？
                //  ——> 因为更新setmeal数据库必须要根据id找到对应setmeal数据，要修改的是status，其它字段都是null不会修改(保留原数据库的数据)
                Setmeal newStemeal = Setmeal.builder()
                        .status(status)
                        .id(id)
                        .build();
                setmealMapper.update(newStemeal);
            }
        } else if (setmeal.getStatus() == StatusConstant.DISABLE) { // 套餐在停售
            if (status == StatusConstant.ENABLE) {   // 要启售套餐（如果套餐中有的菜品在停售，那这个套餐就不能启售）

                // TODO: 查了太多次数据库，可以一次查出数据，后续修改
                List<SetmealDish> list = setmealDishMapper.getDishListBySetmealId(id);
                for (SetmealDish setmealDish : list) {
                    Dish dish = dishMapper.getById(setmealDish.getDishId());
                    if (dish.getStatus() == StatusConstant.DISABLE) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
                Setmeal newSetmeal = Setmeal.builder()
                        .status(status)
                        .id(id)
                        .build();
                setmealMapper.update(newSetmeal);
            }
        }
    }

    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
