package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.Vo.SpuInfoVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * spu信息
 *
 * @author fcy
 * @email lxf@atguigu.com
 * @date 2020-01-23 12:48:39
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageVo queryPage(QueryCondition params);


    PageVo querySpuPage(QueryCondition condition, Long cid);

    void bigSave(SpuInfoVO spuInfoVO);
}

