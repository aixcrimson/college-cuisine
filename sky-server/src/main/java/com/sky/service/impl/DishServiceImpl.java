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
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // 1.准备要添加的菜品数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 2.向菜品表插入1条数据
        dishMapper.insert(dish);
        // 3.获取insert语句生成的主键值
        Long dishId = dish.getId();

        // 4.获取要添加的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 5.向菜品口味关系表表中插入口味数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 条件分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        // 1.判断当前菜品是否在售状态下（不可删除菜品）
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                // 当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 2.判断当前菜品是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            // 当前菜品被套餐关联，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 3.根据菜品id集合批量删除菜品数据 - delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);

        // 4.根据菜品id集合批量删除菜品关联的口味数据 - delete from dish_flavor where dish_id in (?,?,?)
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 1.根据id获取菜品数据
        Dish dish = dishMapper.getById(id);

        // 2.根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 3.封装成dishVO返回
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品信息或对应的口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        // 1.获取要修改的菜品信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 2.修改菜品信息
        dishMapper.update(dish);

        // 3.删除原有的口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 3.获取要修改的口味信息
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 4.重新插入口味数据
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                // 设置菜品口味表的菜品id属性
                dishFlavor.setDishId(dishDTO.getId());
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }
}
