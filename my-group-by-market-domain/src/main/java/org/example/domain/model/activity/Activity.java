package org.example.domain.model.activity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.activity.valueobject.ActivityStatus;
import org.example.domain.model.activity.valueobject.GroupType;
import org.example.domain.model.activity.valueobject.TagScope;

import java.time.LocalDateTime;

/**
 * Activity 聚合根（拼团活动）
 * 职责：管理活动的配置和生命周期
 */
@Slf4j
@Data
public class Activity {

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 活动描述 */
    private String activityDesc;

    /** 默认折扣ID（外部引用） */
    private String discountId;

    /** 人群标签ID（外部引用） */
    private String tagId;

    /**
     * 人群标签作用域
     * 定义非目标人群（不在tagId标签内的用户）的可见性和参与性规则
     * - STRICT: 严格模式，不在标签内不可见不可参与（默认）
     * - VISIBLE_ONLY: 可见模式，不在标签内仅可见不可参与
     * - OPEN: 开放模式，不在标签内可见可参与（慎用）
     */
    private TagScope tagScope;

    /** 成团方式（0=虚拟成团，1=真实成团） */
    private GroupType groupType;

    /** 成团目标人数 */
    private Integer target;

    /** 拼单有效时长（秒） */
    private Integer validTime;

    /** 用户参团次数限制 */
    private Integer participationLimit;

    /** 活动状态 */
    private ActivityStatus status;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 创建活动（工厂方法）
     */
    public static Activity create(
            String activityId,
            String activityName,
            String discountId,
            String tagId,
            TagScope tagScope,
            GroupType groupType,
            Integer target,
            Integer validTime,
            Integer participationLimit,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 强不变式1：折扣ID不能为空
        if (discountId == null || discountId.isEmpty()) {
            throw new BizException("折扣配置不能为空");
        }

        // 强不变式2：时间校验
        if (startTime.isAfter(endTime)) {
            throw new BizException("活动开始时间不能晚于结束时间");
        }

        Activity activity = new Activity();
        activity.activityId = activityId;
        activity.activityName = activityName;
        activity.discountId = discountId;
        activity.tagId = tagId;
        activity.tagScope = tagScope != null ? tagScope : TagScope.STRICT; // 默认严格模式
        activity.groupType = groupType;
        activity.target = target;
        activity.validTime = validTime;
        activity.participationLimit = participationLimit;
        activity.status = ActivityStatus.DRAFT;
        activity.startTime = startTime;
        activity.endTime = endTime;
        activity.createTime = LocalDateTime.now();
        activity.updateTime = LocalDateTime.now();

        log.info("【Activity聚合】活动创建成功, activityId: {}, name: {}", activityId, activityName);
        return activity;
    }

    /**
     * 更新活动信息
     */
    public void update(
            String activityName,
            String activityDesc,
            String discountId,
            String tagId,
            TagScope tagScope,
            GroupType groupType,
            Integer target,
            Integer validTime,
            Integer participationLimit,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 强不变式：折扣ID不能为空
        if (discountId == null || discountId.isEmpty()) {
            throw new BizException("折扣配置不能为空");
        }

        // 强不变式：时间校验
        if (startTime.isAfter(endTime)) {
            throw new BizException("活动开始时间不能晚于结束时间");
        }

        this.activityName = activityName;
        this.activityDesc = activityDesc;
        this.discountId = discountId;
        this.tagId = tagId;
        this.tagScope = tagScope != null ? tagScope : TagScope.STRICT;
        this.groupType = groupType;
        this.target = target;
        this.validTime = validTime;
        this.participationLimit = participationLimit;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updateTime = LocalDateTime.now();

        log.info("【Activity聚合】活动更新成功, activityId: {}", activityId);
    }

    /**
     * 激活活动
     * 状态转换：DRAFT → ACTIVE
     */
    public void activate() {
        // 强不变式3：状态转换规则
        if (this.status != ActivityStatus.DRAFT) {
            throw new BizException("只有草稿状态的活动才能激活");
        }

        this.status = ActivityStatus.ACTIVE;
        this.updateTime = LocalDateTime.now();

        log.info("【Activity聚合】活动已激活, activityId: {}", activityId);
    }

    /**
     * 关闭活动
     * 状态转换：ACTIVE → CLOSED
     */
    public void close() {
        if (this.status != ActivityStatus.ACTIVE) {
            throw new BizException("只有生效中的活动才能关闭");
        }

        this.status = ActivityStatus.CLOSED;
        this.updateTime = LocalDateTime.now();

        log.info("【Activity聚合】活动已关闭, activityId: {}", activityId);
    }

    /**
     * 检查活动是否有效
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return this.status == ActivityStatus.ACTIVE
                && now.isAfter(startTime)
                && now.isBefore(endTime);
    }

    /**
     * 断言活动可用（守卫方法）
     * 
     * <p>
     * 用于交易前的活动可用性校验，确保活动处于可参与状态。
     * 这是一个守卫方法（Guard Method），如果活动不可用会抛出业务异常。
     * 
     * <p>
     * 校验规则：
     * <ul>
     * <li>活动状态必须为 ACTIVE</li>
     * <li>当前时间必须在活动有效期内（startTime ~ endTime）</li>
     * </ul>
     * 
     * @throws BizException 如果活动不可用，抛出带详细信息的业务异常
     */
    public void assertAvailable() {
        // 校验活动状态
        if (this.status != ActivityStatus.ACTIVE) {
            throw new BizException(
                    org.example.common.exception.ErrorCode.ACTIVITY_NOT_ACTIVE,
                    this.status.getDesc());
        }

        // 校验活动时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(this.startTime)) {
            throw new BizException(
                    org.example.common.exception.ErrorCode.ACTIVITY_NOT_STARTED,
                    this.startTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (now.isAfter(this.endTime)) {
            throw new BizException(
                    org.example.common.exception.ErrorCode.ACTIVITY_EXPIRED,
                    this.endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        log.debug("【Activity聚合】活动可用性校验通过, activityId: {}", this.activityId);
    }
}