package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author fcy
 * @email lxf@atguigu.com
 * @date 2020-01-27 14:31:01
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
