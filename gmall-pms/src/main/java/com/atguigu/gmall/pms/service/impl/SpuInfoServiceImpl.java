package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.Vo.BaseAttrVO;
import com.atguigu.gmall.pms.Vo.SkuInfoVO;
import com.atguigu.gmall.pms.Vo.SpuInfoVO;
import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.ProductAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SkuSaleAttrValueService;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDao;
import com.atguigu.gmall.pms.service.SpuInfoService;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescDao descDao;

    @Autowired
    private ProductAttrValueService attrValueService;

    @Autowired
    private SkuInfoDao skuInfoDao;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuSaleAttrValueService saleAttrValueService;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private AmqpTemplate amqpTemplate;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );
        return new PageVo(page);
    }

    @Override
    public PageVo querySpuPage(QueryCondition condition, Long cid) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        //判断分类是否为0
        if(cid != 0){
            wrapper.eq("catalog_id",cid);
        }

        //判断关键字是否为空
        String key = condition.getKey();
        if(StringUtils.isNotBlank(key)){
            wrapper.and(t -> t.eq("id",key).or().like("spu_name",key));
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(condition),wrapper);

        return new PageVo(page);
    }

    @Override
    @GlobalTransactional
    public void bigSave(SpuInfoVO spuInfoVO) {
        //1.保存spu相关的3张表
        //1.1 保存pms_spu_info信息
        spuInfoVO.setCreateTime(new Date());
        spuInfoVO.setUodateTime(spuInfoVO.getCreateTime());
        this.save(spuInfoVO);
        Long spuId = spuInfoVO.getId();

        //1.2保存pms_spu_info_desc
        List<String> spuImages = spuInfoVO.getSpuImages();
        if(! CollectionUtils.isEmpty(spuImages)){
            SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
            descEntity.setSpuId(spuId);
            descEntity.setDecript(StringUtils.join(spuImages,","));
            this.descDao.insert(descEntity);
        }


        //1.3保存pms_product_attr_value
        List<BaseAttrVO> baseAttrs = spuInfoVO.getBaseAttrs();
        if(!CollectionUtils.isEmpty(baseAttrs)){
            List<ProductAttrValueEntity> attrValueEntities = baseAttrs.stream().map(baseAttrVO -> {
                BaseAttrVO attrValueEntity = baseAttrVO;
                attrValueEntity.setSpuId(spuId);
                return attrValueEntity;
            }).collect(Collectors.toList());
            this.attrValueService.saveBatch(attrValueEntities);
        }



        //2.保存sku相关的3张表

        List<SkuInfoVO> skus = spuInfoVO.getSkus();
        if(CollectionUtils.isEmpty(skus)){
            return ;
        }
        skus.forEach(SkuInfoVO -> {
            //2.1 保存pms_sku_info
            SkuInfoVO.setSpuId(spuId);
            SkuInfoVO.setSkuCode(UUID.randomUUID().toString());
            SkuInfoVO.setBrandId(spuInfoVO.getBrandId());
            SkuInfoVO.setCatalogId(spuInfoVO.getCatalogId());
            List<String> images = SkuInfoVO.getImages();
            //设置默认图片
            if(!CollectionUtils.isEmpty(images)){
                SkuInfoVO.setSkuDefaultImg(StringUtils.isNotBlank(SkuInfoVO.getSkuDefaultImg()) ? SkuInfoVO.getSkuDefaultImg() : images.get(0));
            }
            this.skuInfoDao.insert(SkuInfoVO);
            Long skuId = SkuInfoVO.getSkuId();

            //2.2 保存pms_sku_images
            if(!CollectionUtils.isEmpty(images)){
                List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                    SkuImagesEntity imagesEntity = new SkuImagesEntity();
                    imagesEntity.setImgUrl(image);
                    imagesEntity.setSkuId(skuId);
                    //设置是否默认图片
                    imagesEntity.setDefaultImg(StringUtils.equals(SkuInfoVO.getSkuDefaultImg(), image) ? 1 : 0);
                    return imagesEntity;
                }).collect(Collectors.toList());
                this.imagesService.saveBatch(skuImagesEntities);
            }

            //2.3 保存pms_sale_attr_value
            List<SkuSaleAttrValueEntity> saleAttrs = SkuInfoVO.getSaleAttrs();
            if(!CollectionUtils.isEmpty(saleAttrs)){
                //设置skuid
                saleAttrs.forEach(skuSaleAttrValueEntity -> skuSaleAttrValueEntity.setSkuId(skuId));
                //批量保存销售属性
                this.saleAttrValueService.saveBatch(saleAttrs);
            }

            //3.保存营销信息的3张表(feign远程调用sms)
            SkuSaleVO skuSaleVO = new SkuSaleVO();
            BeanUtils.copyProperties(SkuInfoVO,skuSaleVO);
            skuSaleVO.setSkuId(skuId);
            this.smsClient.saveSale(skuSaleVO);
        });




    }

}