package org.example.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.tag.repository.CrowdTagRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 人群标签计算定时任务
 *
 * 依赖关系：
 * Application 层 → Domain 层接口（CrowdTagRepository）
 * Infrastructure 层实现接口（CrowdTagRepositoryImpl）
 * Spring 自动注入实现类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrowdTagCalculateJob {

    private final CrowdTagRepository crowdTagRepository;

    /**
     * 定时计算人群标签
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void calculateCrowdTag() {
        log.info("【人群标签任务】开始计算人群标签");

        try {
            // 1. 执行标签计算逻辑
            String tagId = "TAG001";
            List<String> userIds = queryUsersMatchingTag(tagId);

            if (userIds.isEmpty()) {
                log.warn("【人群标签任务】未查询到符合条件的用户, tagId: {}", tagId);
                return;
        }

            // 2. 全量替换标签用户（清除旧数据，写入新数据）
            crowdTagRepository.replaceTagUsers(tagId, userIds);

            log.info("【人群标签任务】人群标签计算完成, tagId: {}, count: {}",
                    tagId, userIds.size());

        } catch (Exception e) {
            log.error("【人群标签任务】人群标签计算失败", e);
        }
    }

    /**
     * 查询符合标签条件的用户
     *
     * 实际业务逻辑：
     * - 从用户表查询活跃用户
     * - 从订单表查询消费金额 > 1000 的用户
     * - 从会员表查询 VIP 用户
     * 等等...
     */
    private List<String> queryUsersMatchingTag(String tagId) {
        // TODO: 实现真实的标签计算逻辑
        // 这里仅作示例
        return List.of("user001", "user002", "user003", "user004", "user005");
    }

    /**
     * 统计标签用户数量（示例）
     */
    public Long getTagUserCount(String tagId) {
        return crowdTagRepository.countUsersByTagId(tagId);
    }


    /**
     * 删除标签（示例）
     */
    public void deleteTag(String tagId) {
        crowdTagRepository.deleteTag(tagId);
        log.info("【人群标签任务】标签已删除, tagId: {}", tagId);
    }
}