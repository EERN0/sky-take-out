package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品：
     * 先判断redis缓存中有无分类id的数据，没有再去查mysql，并写入缓存
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        // 构造redis中的key，规则：dish_分类id
        String key = "dish_" + categoryId;

        // 1.查询redis是否有菜品数据
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);

        // 2.如果存在，直接返回数据
        if (list != null && !list.isEmpty()) {
            return Result.success(list);
        }

        // 3.redis不存在该数据，查mysql，并将查询到的数据放入redis中
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);  //查询起售中的菜品

        list = dishService.listWithFlavor(dish);    // 查数据库
        redisTemplate.opsForValue().set(key, list); // 把从mysql查出来的数据放到redis缓存中

        return Result.success(list);
    }
}
