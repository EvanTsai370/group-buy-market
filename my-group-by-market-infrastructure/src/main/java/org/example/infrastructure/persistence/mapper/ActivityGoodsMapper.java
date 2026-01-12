package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.ActivityGoodsPO;

import java.util.List;

/**
 * 活动商品关联Mapper
 */
@Mapper
public interface ActivityGoodsMapper extends BaseMapper<ActivityGoodsPO> {

        /**
         * 根据商品ID、来源、渠道查询活动ID
         * 用于试算场景：根据商品和渠道定位活动
         *
         * @param skuId   商品ID
         * @param source  来源
         * @param channel 渠道
         * @return 活动ID（可能为空）
         */
        String selectActivityIdByGoodsSourceChannel(@Param("spuId") String spuId,
                        @Param("source") String source,
                        @Param("channel") String channel);

        /**
         * 根据活动ID查询所有关联商品
         *
         * @param activityId 活动ID
         * @return 活动商品关联列表
         */
        List<ActivityGoodsPO> selectByActivityId(@Param("activityId") String activityId);

        /**
         * 根据活动ID和商品ID查询关联信息
         *
         * @param activityId 活动ID
         * @param spuId      商品ID
         * @param source     来源
         * @param channel    渠道
         * @return 活动商品关联
         */
        ActivityGoodsPO selectByActivityGoods(@Param("activityId") String activityId,
                        @Param("spuId") String spuId,
                        @Param("source") String source,
                        @Param("channel") String channel);

        /**
         * 根据商品ID查询有效活动ID
         * 用于 C 端商品详情页：展示商品关联的拼团活动
         *
         * @param spuId 商品ID
         * @return 活动ID列表（可能有多个活动关联同一商品）
         */
        List<String> selectActiveActivityIdsBySkuId(@Param("spuId") String spuId);
}
