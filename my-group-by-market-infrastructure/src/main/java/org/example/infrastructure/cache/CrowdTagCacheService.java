// ============ 文件: CrowdTagCacheService.java ============
package org.example.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 人群标签缓存服务
 * 使用 Redis Set 结构存储标签用户关系
 *
 * 数据结构：
 * Key: crowd:tag:{tagId}
 * Value: Set<userId>
 *
 * 优势：
 * 1. SISMEMBER 命令 O(1) 复杂度，查询极快
 * 2. Set 结构天然去重
 * 3. 支持批量操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrowdTagCacheService {

    private final StringRedisTemplate redisTemplate;

    /** Redis Key 前缀 */
    private static final String TAG_USER_KEY_PREFIX = "crowd:tag:";

    /** 缓存过期时间（小时） */
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 获取标签用户集合的 Redis Key
     */
    private String getTagUserKey(String tagId) {
        return TAG_USER_KEY_PREFIX + tagId;
    }

    /**
     * 检查用户是否在标签内（使用 Redis）
     *
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return true=在标签内，false=不在标签内
     */
    public Boolean checkUserInTag(String userId, String tagId) {
        try {
            String key = getTagUserKey(tagId);
            Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);

            log.debug("【标签缓存】检查用户, userId: {}, tagId: {}, exists: {}",
                    userId, tagId, isMember);

            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("【标签缓存】检查用户失败, userId: {}, tagId: {}", userId, tagId, e);
            return null;  // 返回 null 表示缓存查询失败，调用方降级到数据库
        }
    }

    /**
     * 全量替换标签用户（同步到 Redis）
     * 先删除旧数据，再写入新数据
     *
     * @param tagId 标签ID
     * @param userIds 用户ID列表
     */
    public void replaceTagUsers(String tagId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("【标签缓存】用户列表为空, tagId: {}", tagId);
            return;
        }

        try {
            String key = getTagUserKey(tagId);

            // 先删除旧数据（保证幂等性）
            redisTemplate.delete(key);

            // 批量添加（Redis Set 的 SADD 命令支持多个值）
            redisTemplate.opsForSet().add(key, userIds.toArray(new String[0]));

            // 设置过期时间
            redisTemplate.expire(key, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("【标签缓存】批量添加用户成功, tagId: {}, count: {}", tagId, userIds.size());
        } catch (Exception e) {
            log.error("【标签缓存】批量添加用户失败, tagId: {}, count: {}", tagId, userIds.size(), e);
            throw new RuntimeException("标签缓存写入失败", e);
        }
    }

    /**
     * 获取标签下的所有用户（从 Redis）
     *
     * @param tagId 标签ID
     * @return 用户ID集合
     */
    public Set<String> getUserIdsByTagId(String tagId) {
        try {
            String key = getTagUserKey(tagId);
            Set<String> userIds = redisTemplate.opsForSet().members(key);

            if (userIds == null) {
                userIds = Collections.emptySet();
            }

            log.debug("【标签缓存】查询标签用户, tagId: {}, count: {}", tagId, userIds.size());

            return userIds;
        } catch (Exception e) {
            log.error("【标签缓存】查询标签用户失败, tagId: {}", tagId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 统计标签用户数量（从 Redis）
     *
     * @param tagId 标签ID
     * @return 用户数量
     */
    public Long countUsersByTagId(String tagId) {
        try {
            String key = getTagUserKey(tagId);
            Long count = redisTemplate.opsForSet().size(key);

            log.debug("【标签缓存】统计标签用户, tagId: {}, count: {}", tagId, count);

            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("【标签缓存】统计标签用户失败, tagId: {}", tagId, e);
            return 0L;
        }
    }

    /**
     * 删除标签缓存
     *
     * @param tagId 标签ID
     */
    public void deleteTagCache(String tagId) {
        try {
            String key = getTagUserKey(tagId);
            redisTemplate.delete(key);
            log.info("【标签缓存】删除标签缓存成功, tagId: {}", tagId);
        } catch (Exception e) {
            log.error("【标签缓存】删除标签缓存失败, tagId: {}", tagId, e);
        }
    }

    /**
     * 刷新标签缓存（重新加载）
     *
     * @param tagId 标签ID
     * @param userIds 用户ID列表
     */
    public void refreshTagCache(String tagId, List<String> userIds) {
        try {
            replaceTagUsers(tagId, userIds);
            log.info("【标签缓存】刷新标签缓存成功, tagId: {}, count: {}", tagId, userIds.size());
        } catch (Exception e) {
            log.error("【标签缓存】刷新标签缓存失败, tagId: {}", tagId, e);
        }
    }

    /**
     * 检查标签缓存是否存在
     *
     * @param tagId 标签ID
     * @return true=存在，false=不存在
     */
    public Boolean existsTagCache(String tagId) {
        try {
            String key = getTagUserKey(tagId);
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("【标签缓存】检查缓存是否存在失败, tagId: {}", tagId, e);
            return false;
        }
    }

    /**
     * 批量检查用户是否在标签内
     *
     * @param userIds 用户ID列表
     * @param tagId 标签ID
     * @return 在标签内的用户ID列表
     */
    public List<String> batchCheckUsersInTag(List<String> userIds, String tagId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String key = getTagUserKey(tagId);

            // 使用 pipeline 批量查询（性能优化）
            return userIds.stream()
                    .filter(userId -> {
                        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
                        return isMember != null && isMember;
                    })
                    .toList();
        } catch (Exception e) {
            log.error("【标签缓存】批量检查用户失败, tagId: {}, count: {}",
                    tagId, userIds.size(), e);
            return Collections.emptyList();
        }
    }
}