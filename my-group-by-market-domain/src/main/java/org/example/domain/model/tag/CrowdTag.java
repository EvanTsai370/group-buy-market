package org.example.domain.model.tag;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.tag.valueobject.TagStatus;

import java.time.LocalDateTime;

/**
 * CrowdTag 聚合根（人群标签）
 * 职责：管理人群标签的定义和计算任务
 */
@Slf4j
@Data
public class CrowdTag {

    /** 标签ID */
    private String tagId;

    /** 标签名称 */
    private String tagName;

    /** 标签描述 */
    private String tagDesc;

    /** 标签规则（JSON表达式） */
    private String tagRule;

    /** 符合人数统计 */
    private Long statistics;

    /** 标签状态 */
    private TagStatus status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 创建标签（工厂方法）
     */
    public static CrowdTag create(String tagId, String tagName, String tagDesc, String tagRule) {
        CrowdTag tag = new CrowdTag();
        tag.tagId = tagId;
        tag.tagName = tagName;
        tag.tagDesc = tagDesc;
        tag.tagRule = tagRule;
        tag.statistics = 0L;
        tag.status = TagStatus.DRAFT;
        tag.createTime = LocalDateTime.now();
        tag.updateTime = LocalDateTime.now();

        log.info("【CrowdTag聚合】标签创建成功, tagId: {}, name: {}", tagId, tagName);
        return tag;
    }

    /**
     * 开始计算
     * 状态转换：DRAFT → CALCULATING
     */
    public void startCalculating() {
        if (this.status != TagStatus.DRAFT) {
            throw new BizException("只有草稿状态的标签才能开始计算");
        }

        this.status = TagStatus.CALCULATING;
        this.updateTime = LocalDateTime.now();

        log.info("【CrowdTag聚合】开始计算人群, tagId: {}", tagId);
    }

    /**
     * 计算完成
     * 状态转换：CALCULATING → COMPLETED
     */
    public void completeCalculation(Long statistics) {
        if (this.status != TagStatus.CALCULATING) {
            throw new BizException("只有计算中的标签才能完成计算");
        }

        this.status = TagStatus.COMPLETED;
        this.statistics = statistics;
        this.updateTime = LocalDateTime.now();

        log.info("【CrowdTag聚合】人群计算完成, tagId: {}, statistics: {}", tagId, statistics);
    }

    /**
     * 计算失败
     * 状态转换：CALCULATING → FAILED
     */
    public void failCalculation(String reason) {
        this.status = TagStatus.FAILED;
        this.updateTime = LocalDateTime.now();

        log.error("【CrowdTag聚合】人群计算失败, tagId: {}, reason: {}", tagId, reason);
    }

    /**
     * 检查标签是否可用
     * 强不变式：只有 COMPLETED 状态的标签可被使用
     */
    public boolean isAvailable() {
        return this.status == TagStatus.COMPLETED;
    }

    /**
     * 初始化
     */
    public void init() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新
     */
    public void update() {
        this.updateTime = LocalDateTime.now();
    }
}