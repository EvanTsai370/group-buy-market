package org.example.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 人群标签缓存服务
 * 使用 Redis BitMap 结构存储标签用户关系
 *
 * 数据结构：
 * Key: crowd:tag:bitmap:{tagId}
 * Value: BitMap（位图）
 *
 * 优势：
 * 1. 极低内存占用：100万用户仅需约 125KB（vs Set 约 50MB）
 * 2. GETBIT 命令 O(1) 复杂度
 * 3. 支持 BITOP 进行标签交集/并集运算
 *
 * 注意：
 * 1. userId 需通过哈希转换为整数索引
 * 2. 存在极小概率的哈希冲突（可接受的误判）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrowdTagCacheService {

    private final IRedisService redisService;

    /** Redis Key 前缀 */
    private static final String TAG_BITMAP_KEY_PREFIX = "crowd:tag:bitmap:";

    /** 缓存过期时间（小时） */
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 获取标签 BitMap 的 Redis Key
     */
    private String getTagBitmapKey(String tagId) {
        return TAG_BITMAP_KEY_PREFIX + tagId;
    }

    /**
     * 检查用户是否在标签内（使用 BitMap）
     *
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return true=在标签内，false=不在标签内，null=查询失败
     */
    public Boolean checkUserInTag(String userId, String tagId) {
        try {
            String key = getTagBitmapKey(tagId);
            RBitSet bitSet = redisService.getBitSet(key);
            long index = redisService.getIndexFromUserId(userId);

            boolean exists = bitSet.get(index);

            log.debug("【标签缓存】检查用户(BitMap), userId: {}, tagId: {}, index: {}, exists: {}",
                    userId, tagId, index, exists);

            return exists;
        } catch (Exception e) {
            log.error("【标签缓存】检查用户失败, userId: {}, tagId: {}", userId, tagId, e);
            return null;  // 返回 null 表示缓存查询失败，调用方降级到数据库
        }
    }

    /**
     * 添加单个用户到标签 BitMap
     *
     * @param tagId 标签ID
     * @param userId 用户ID
     */
    public void addUserToTag(String tagId, String userId) {
        try {
            String key = getTagBitmapKey(tagId);
            RBitSet bitSet = redisService.getBitSet(key);
            long index = redisService.getIndexFromUserId(userId);

            bitSet.set(index, true);

            log.debug("【标签缓存】添加用户(BitMap), tagId: {}, userId: {}, index: {}",
                    tagId, userId, index);
        } catch (Exception e) {
            log.error("【标签缓存】添加用户失败, tagId: {}, userId: {}", tagId, userId, e);
        }
    }

    /**
     * 全量替换标签用户（同步到 Redis BitMap）
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
            String key = getTagBitmapKey(tagId);

            // 先删除旧数据（保证幂等性）
            redisService.delete(key);

            // 获取 BitSet 并批量设置
            RBitSet bitSet = redisService.getBitSet(key);
            for (String userId : userIds) {
                long index = redisService.getIndexFromUserId(userId);
                bitSet.set(index, true);
            }

            // 设置过期时间
            redisService.expire(key, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("【标签缓存】批量添加用户成功(BitMap), tagId: {}, count: {}", tagId, userIds.size());
        } catch (Exception e) {
            log.error("【标签缓存】批量添加用户失败, tagId: {}, count: {}", tagId, userIds.size(), e);
            throw new RuntimeException("标签缓存写入失败", e);
        }
    }

    /**
     * 统计标签用户数量（从 BitMap）
     * 返回 BitMap 中值为 1 的位数
     *
     * @param tagId 标签ID
     * @return 用户数量（近似值，因存在哈希冲突可能略小于实际值）
     */
    public Long countUsersByTagId(String tagId) {
        try {
            String key = getTagBitmapKey(tagId);
            RBitSet bitSet = redisService.getBitSet(key);

            // cardinality() 返回 BitMap 中 1 的个数
            long count = bitSet.cardinality();

            log.debug("【标签缓存】统计标签用户(BitMap), tagId: {}, count: {}", tagId, count);

            return count;
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
            String key = getTagBitmapKey(tagId);
            redisService.delete(key);
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
            String key = getTagBitmapKey(tagId);
            RBitSet bitSet = redisService.getBitSet(key);
            // BitMap 存在且不为空
            return bitSet.isExists() && bitSet.cardinality() > 0;
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
            String key = getTagBitmapKey(tagId);
            RBitSet bitSet = redisService.getBitSet(key);

            return userIds.stream()
                    .filter(userId -> {
                        long index = redisService.getIndexFromUserId(userId);
                        return bitSet.get(index);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("【标签缓存】批量检查用户失败, tagId: {}, count: {}",
                    tagId, userIds.size(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 计算两个标签的交集用户数量
     * 场景：统计同时属于「高消费」和「活跃用户」的人数
     *
     * @param tagId1 标签1
     * @param tagId2 标签2
     * @return 交集用户数量
     */
    public Long countIntersection(String tagId1, String tagId2) {
        try {
            String key1 = getTagBitmapKey(tagId1);
            String key2 = getTagBitmapKey(tagId2);

            // 创建临时 BitSet 存储交集结果
            String tempKey = TAG_BITMAP_KEY_PREFIX + "temp:and:" + tagId1 + ":" + tagId2;
            RBitSet tempBitSet = redisService.getBitSet(tempKey);

            // 执行 AND 操作（直接使用 key 名称）
            tempBitSet.and(key1, key2);
            long count = tempBitSet.cardinality();

            // 删除临时 key
            redisService.delete(tempKey);

            log.debug("【标签缓存】计算标签交集, tagId1: {}, tagId2: {}, count: {}",
                    tagId1, tagId2, count);

            return count;
        } catch (Exception e) {
            log.error("【标签缓存】计算标签交集失败, tagId1: {}, tagId2: {}", tagId1, tagId2, e);
            return 0L;
        }
    }

    /**
     * 计算两个标签的并集用户数量
     *
     * @param tagId1 标签1
     * @param tagId2 标签2
     * @return 并集用户数量
     */
    public Long countUnion(String tagId1, String tagId2) {
        try {
            String key1 = getTagBitmapKey(tagId1);
            String key2 = getTagBitmapKey(tagId2);

            // 创建临时 BitSet 存储并集结果
            String tempKey = TAG_BITMAP_KEY_PREFIX + "temp:or:" + tagId1 + ":" + tagId2;
            RBitSet tempBitSet = redisService.getBitSet(tempKey);

            // 执行 OR 操作
            tempBitSet.or(key1, key2);
            long count = tempBitSet.cardinality();

            // 删除临时 key
            redisService.delete(tempKey);

            log.debug("【标签缓存】计算标签并集, tagId1: {}, tagId2: {}, count: {}",
                    tagId1, tagId2, count);

            return count;
        } catch (Exception e) {
            log.error("【标签缓存】计算标签并集失败, tagId1: {}, tagId2: {}", tagId1, tagId2, e);
            return 0L;
        }
    }
}