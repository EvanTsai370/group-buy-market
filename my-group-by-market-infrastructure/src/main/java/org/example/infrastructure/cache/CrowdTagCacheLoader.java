package org.example.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.persistence.mapper.CrowdTagDetailMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 人群标签缓存异步加载器
 * 用于异步加载标签缓存，避免阻塞主流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrowdTagCacheLoader {

    private final CrowdTagDetailMapper crowdTagDetailMapper;
    private final CrowdTagCacheService cacheService;

    /**
     * 异步加载标签缓存
     * 从数据库查询标签用户并写入 Redis 缓存
     *
     * @param tagId 标签ID
     */
    @Async("commonExecutor")
    public void loadTagCacheAsync(String tagId) {
        try {
            // 检查缓存是否已存在
            if (cacheService.existsTagCache(tagId)) {
                log.debug("【CrowdTagCacheLoader】标签缓存已存在，跳过加载, tagId: {}", tagId);
                return;
            }

            // 从数据库加载用户列表
            List<String> userIds = crowdTagDetailMapper.selectUserIdsByTagId(tagId);
            if (userIds.isEmpty()) {
                log.debug("【CrowdTagCacheLoader】标签无用户数据，跳过加载, tagId: {}", tagId);
                return;
            }

            // 写入缓存
            cacheService.replaceTagUsers(tagId, userIds);
            log.info("【CrowdTagCacheLoader】异步加载标签缓存完成, tagId: {}, count: {}",
                    tagId, userIds.size());

        } catch (Exception e) {
            log.error("【CrowdTagCacheLoader】异步加载标签缓存失败, tagId: {}", tagId, e);
        }
    }
}
