package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.tag.CrowdTag;
import org.example.domain.model.tag.repository.CrowdTagRepository;
import org.example.domain.shared.IdGenerator;
import org.example.infrastructure.cache.CrowdTagCacheLoader;
import org.example.infrastructure.cache.CrowdTagCacheService;
import org.example.infrastructure.persistence.converter.CrowdTagConverter;
import org.example.infrastructure.persistence.mapper.CrowdTagDetailMapper;
import org.example.infrastructure.persistence.mapper.CrowdTagMapper;
import org.example.infrastructure.persistence.po.CrowdTagDetailPO;
import org.example.infrastructure.persistence.po.CrowdTagPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CrowdTag 仓储实现（加入 Redis 缓存）
 *
 * 缓存策略：
 * 1. 优先查询 Redis 缓存
 * 2. 缓存未命中时查询数据库并自动加载缓存
 * 3. 写入时双写（数据库 + Redis）
 * 4. Redis 故障时自动降级到数据库
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrowdTagRepositoryImpl implements CrowdTagRepository {

    private final CrowdTagMapper crowdTagMapper;
    private final CrowdTagDetailMapper crowdTagDetailMapper;
    private final CrowdTagCacheService cacheService;
    private final CrowdTagCacheLoader cacheLoader;
    private final IdGenerator idGenerator;

    @Override
    public void save(CrowdTag tag) {
        CrowdTagPO po = CrowdTagConverter.INSTANCE.toPO(tag);

        if (po.getId() == null) {
            // 新增
            crowdTagMapper.insert(po);
            log.info("【CrowdTagRepository】新增标签, tagId: {}", tag.getTagId());
        } else {
            // 更新
            crowdTagMapper.updateById(po);
            log.info("【CrowdTagRepository】更新标签, tagId: {}", tag.getTagId());
        }
    }

    @Override
    public Optional<CrowdTag> findById(String tagId) {
        LambdaQueryWrapper<CrowdTagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrowdTagPO::getTagId, tagId);

        CrowdTagPO po = crowdTagMapper.selectOne(wrapper);
        if (po == null) {
            return Optional.empty();
        }

        CrowdTag tag = CrowdTagConverter.INSTANCE.toDomain(po);
        return Optional.of(tag);
    }

    @Override
    public Boolean checkUserInTag(String userId, String tagId) {
        // 1. 先查 Redis 缓存
        try {
            Boolean cached = cacheService.checkUserInTag(userId, tagId);
            if (cached != null) {
                log.debug("【CrowdTagRepository】缓存命中, userId: {}, tagId: {}, exists: {}",
                        userId, tagId, cached);
                return cached;
            }
        } catch (Exception e) {
            log.warn("【CrowdTagRepository】Redis查询失败，降级到数据库, tagId: {}", tagId, e);
        }

        // 2. 缓存未命中或失败，查询数据库
        int count = crowdTagDetailMapper.checkUserInTag(userId, tagId);
        boolean exists = count > 0;

        log.debug("【CrowdTagRepository】数据库查询结果, userId: {}, tagId: {}, exists: {}",
                userId, tagId, exists);

        // 3. 尝试加载标签缓存（异步或同步都可以）
        tryLoadTagCacheAsync(tagId);

        return exists;
    }

    @Override
    public String nextId() {
        return "TAG" + idGenerator.nextId();
    }

    /**
     * 全量替换标签用户（同时写入数据库和缓存）
     * 先删除旧数据，再插入新数据，保证幂等性
     *
     * @param tagId 标签ID
     * @param userIds 用户ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceTagUsers(String tagId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("【CrowdTagRepository】用户列表为空, tagId: {}", tagId);
            return;
        }

        log.info("【CrowdTagRepository】开始批量添加用户到标签, tagId: {}, count: {}",
                tagId, userIds.size());

        // 1. 删除旧数据（保证幂等性）
        crowdTagDetailMapper.deleteByTagId(tagId);
        log.debug("【CrowdTagRepository】已删除旧数据, tagId: {}", tagId);

        // 2. 批量插入新数据
        List<CrowdTagDetailPO> detailList = userIds.stream()
                .map(userId -> CrowdTagDetailPO.builder()
                        .tagId(tagId)
                        .userId(userId)
                        .build())
                .collect(Collectors.toList());

        // 分批插入（每批1000条）
        int batchSize = 1000;
        for (int i = 0; i < detailList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, detailList.size());
            List<CrowdTagDetailPO> subList = detailList.subList(i, end);
            crowdTagDetailMapper.batchInsert(subList);
            log.debug("【CrowdTagRepository】批量插入进度, tagId: {}, progress: {}/{}",
                    tagId, end, detailList.size());
        }

        // 3. 更新标签统计数
        CrowdTagPO tagPO = new CrowdTagPO();
        tagPO.setStatistics((long) userIds.size());

        LambdaQueryWrapper<CrowdTagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrowdTagPO::getTagId, tagId);
        crowdTagMapper.update(tagPO, wrapper);
        log.debug("【CrowdTagRepository】已更新标签统计, tagId: {}, count: {}",
                tagId, userIds.size());

        // 4. 同步到 Redis 缓存
        try {
            cacheService.replaceTagUsers(tagId, userIds);
            log.info("【CrowdTagRepository】标签数据已同步到缓存, tagId: {}, count: {}",
                    tagId, userIds.size());
        } catch (Exception e) {
            // Redis 写入失败不影响数据库事务
            log.error("【CrowdTagRepository】同步缓存失败（不影响数据库事务）, tagId: {}", tagId, e);
        }

        log.info("【CrowdTagRepository】批量添加用户到标签完成, tagId: {}, count: {}",
                tagId, userIds.size());
    }

    /**
     * 获取标签下的所有用户
     *
     * @param tagId 标签ID
     * @return 用户ID列表
     */
    public List<String> getUserIdsByTagId(String tagId) {
        // 1. 先查 Redis
        try {
            if (cacheService.existsTagCache(tagId)) {
                var userIds = cacheService.getUserIdsByTagId(tagId);
                if (!userIds.isEmpty()) {
                    log.debug("【CrowdTagRepository】从缓存查询标签用户, tagId: {}, count: {}",
                            tagId, userIds.size());
                    return userIds.stream().toList();
                }
            }
        } catch (Exception e) {
            log.warn("【CrowdTagRepository】Redis查询失败，降级到数据库, tagId: {}", tagId, e);
        }

        // 2. 查询数据库
        List<String> userIds = crowdTagDetailMapper.selectUserIdsByTagId(tagId);
        log.debug("【CrowdTagRepository】从数据库查询标签用户, tagId: {}, count: {}",
                tagId, userIds.size());

        // 3. 加载到缓存（异步）
        if (!userIds.isEmpty()) {
            tryLoadTagCacheAsync(tagId);
        }

        return userIds;
    }

    /**
     * 统计标签用户数量
     *
     * @param tagId 标签ID
     * @return 用户数量
     */
    public Long countUsersByTagId(String tagId) {
        // 1. 先查 Redis
        try {
            if (cacheService.existsTagCache(tagId)) {
                Long count = cacheService.countUsersByTagId(tagId);
                log.debug("【CrowdTagRepository】从缓存统计标签用户, tagId: {}, count: {}",
                        tagId, count);
                return count;
            }
        } catch (Exception e) {
            log.warn("【CrowdTagRepository】Redis统计失败，降级到数据库, tagId: {}", tagId, e);
        }

        // 2. 查询数据库
        Long count = crowdTagDetailMapper.countUsersByTagId(tagId);
        log.debug("【CrowdTagRepository】从数据库统计标签用户, tagId: {}, count: {}", tagId, count);

        return count;
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
            return List.of();
        }

        // 1. 先查 Redis
        try {
            if (cacheService.existsTagCache(tagId)) {
                List<String> validUserIds = cacheService.batchCheckUsersInTag(userIds, tagId);
                log.debug("【CrowdTagRepository】从缓存批量检查用户, tagId: {}, input: {}, valid: {}",
                        tagId, userIds.size(), validUserIds.size());
                return validUserIds;
            }
        } catch (Exception e) {
            log.warn("【CrowdTagRepository】Redis批量检查失败，降级到数据库, tagId: {}", tagId, e);
        }

        // 2. 查询数据库
        List<String> validUserIds = crowdTagDetailMapper.batchCheckUsersInTag(userIds, tagId);
        log.debug("【CrowdTagRepository】从数据库批量检查用户, tagId: {}, input: {}, valid: {}",
                tagId, userIds.size(), validUserIds.size());

        // 3. 尝试加载缓存
        tryLoadTagCacheAsync(tagId);

        return validUserIds;
    }

    /**
     * 删除标签（同时删除缓存）
     *
     * @param tagId 标签ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(String tagId) {
        // 1. 删除数据库数据
        LambdaQueryWrapper<CrowdTagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrowdTagPO::getTagId, tagId);
        crowdTagMapper.delete(wrapper);

        crowdTagDetailMapper.deleteByTagId(tagId);

        // 2. 删除缓存
        try {
            cacheService.deleteTagCache(tagId);
            log.info("【CrowdTagRepository】删除标签及缓存, tagId: {}", tagId);
        } catch (Exception e) {
            log.error("【CrowdTagRepository】删除缓存失败, tagId: {}", tagId, e);
        }
    }

    /**
     * 异步加载标签缓存
     * 委托给 CrowdTagCacheLoader 异步执行，避免阻塞主流程
     *
     * @param tagId 标签ID
     */
    private void tryLoadTagCacheAsync(String tagId) {
        cacheLoader.loadTagCacheAsync(tagId);
    }
}