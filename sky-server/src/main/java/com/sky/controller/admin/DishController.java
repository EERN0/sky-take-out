package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * 新增完菜品后要清理缓存数据(新增会添加菜品分类，分类id对应的菜品会加一个，所以得删掉缓存中的dish_分类id对应的菜品)
     *
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result addDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}", dishDTO);
        dishService.addDishWithFlavor(dishDTO);

        // 清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        //redisTemplate.delete(key);
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询, 参数: {}", dishPageQueryDTO);

        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品的批量删除 --- 菜品id集合有多个分类id，简单起见，直接将所有菜品缓存数据清理掉
     * <p>
     * TODO：只删除菜品id对应的分类数据，不是把redis的分类数据全删了
     * <p>
     * 删除规则：一次可以删一个或者多个;
     * 启售中的菜品不能删除;
     * 被套餐关联的菜品不能删除;
     * 删除菜品后，关联的口味数据也需要删除掉
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品的批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("菜品的批量删除: {}", ids);
        dishService.deleteBatch(ids);

        // 将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }


    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品: {}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);

        return Result.success(dishVO);
    }

    /**
     * 修改菜品 -- 这里也是直接将缓存中所有分类id全删掉
     *
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品: {}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        // 之前清理掉redis中所有以dish_开头的数据
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 菜品的起售停售 --- 简单起见，将所有的菜品缓存数据清理掉，
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品的起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        log.info("菜品的起售、停售: {},{}", id, status);
        dishService.startOrStop(status, id);

        // 将所有的菜品缓存数据清理掉，redis中以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 根据分类id查询相关菜品列表
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getByCategoryId(Long categoryId) {
        List<Dish> dishList = dishService.list(categoryId);
        return Result.success(dishList);
    }


    /**
     * 清理缓存数据
     * @param pattern
     */
    private void cleanCache(String pattern){
        // 将redis中所有缓存数据清理掉，redis中以pattern开头的key
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}

