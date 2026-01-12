package org.example.application.service.customer.result;

import lombok.Data;

import java.math.BigDecimal;

/**
 * SPU列表项结果
 *
 * @author 开发团队
 * @since 2026-01-12
 */
@Data
public class SpuListResult {

    /** SPU ID */
    private String spuId;

    /** SPU 名称 */
    private String spuName;

    /** 主图 */
    private String mainImage;

    /** 最低原价（展示用） */
    private BigDecimal minOriginalPrice;

    /** 最低拼团价（展示用） */
    private BigDecimal minGroupPrice;

    /** 是否有拼团活动 */
    private Boolean hasActivity;

    /** 活动ID（如有活动） */
    private String activityId;
}
