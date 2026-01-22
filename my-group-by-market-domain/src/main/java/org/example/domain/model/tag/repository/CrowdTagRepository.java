package org.example.domain.model.tag.repository;

import org.example.domain.model.tag.CrowdTag;

import java.util.List;
import java.util.Optional;

/**
 * CrowdTag 仓储接口
 */
public interface CrowdTagRepository {

    /**
     * 保存标签
     *
     * @param tag 标签聚合
     */
    void save(CrowdTag tag);

    /**
     * 根据ID查找标签
     *
     * @param tagId 标签ID
     * @return 标签聚合
     */
    Optional<CrowdTag> findById(String tagId);

    /**
     * 检查用户是否在人群标签内
     *
     * @param userId 用户ID
     * @param tagId  标签ID
     * @return 是否在标签内
     */
    Boolean checkUserInTag(String userId, String tagId);

    /**
     * 生成下一个标签ID
     *
     * @return 标签ID
     */
    String nextId();

    /**
     * 全量替换标签用户
     * 用于人群标签计算完成后，清除旧数据并写入新的用户列表
     *
     * @param tagId   标签ID
     * @param userIds 用户ID列表
     */
    void replaceTagUsers(String tagId, List<String> userIds);

    /**
     * 获取标签下的所有用户
     *
     * @param tagId 标签ID
     * @return 用户ID列表
     */
    List<String> getUserIdsByTagId(String tagId);

    /**
     * 统计标签用户数量
     *
     * @param tagId 标签ID
     * @return 用户数量
     */
    Long countUsersByTagId(String tagId);

    /**
     * 批量检查用户是否在标签内
     *
     * @param userIds 用户ID列表
     * @param tagId   标签ID
     * @return 在标签内的用户ID列表
     */
    List<String> batchCheckUsersInTag(List<String> userIds, String tagId);

    /**
     * 删除标签（包括明细和缓存）
     *
     * @param tagId 标签ID
     */
    void deleteTag(String tagId);

    /**
     * 分页查询标签
     * 
     * @param page    页码
     * @param size    每页大小
     * @param keyword 关键词
     * @param status  状态
     * @return 分页结果
     */
    org.example.common.model.PageResult<CrowdTag> findByPage(int page, int size, String keyword, String status);
}