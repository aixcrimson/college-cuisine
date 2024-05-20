package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.UserService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "用户端-菜品浏览接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 1.构造存入redis中的key
        String key = "dish_" + categoryId;
        // 2.查询reids中是否存在菜品缓存
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        // 3.如果存在，直接返回
        if (list != null && list.size() > 0) {
            return Result.success(list);
        }
        // 4.如果不存在，查询数据库
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        list = dishService.listWithFlavor(dish);
        // 5.将数据库查询到的数据缓存到redis
        redisTemplate.opsForValue().set(key, list);
        return Result.success(list);
    }
}
