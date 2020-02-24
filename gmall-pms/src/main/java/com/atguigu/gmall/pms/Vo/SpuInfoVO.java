package com.atguigu.gmall.pms.Vo;


import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import lombok.Data;

import java.util.List;

/**
 * Created by fcy on 2020/2/2.
 */
@Data
public class SpuInfoVO extends SpuInfoEntity {

     private List<String> spuImages;

     private List<BaseAttrVO> baseAttrs;

     private List<SkuInfoVO> skus;



}
