// ============ 文件: CrowdTagDetailMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.CrowdTagDetailPO;

import java.util.List;

/**
 * 人群标签明细 Mapper
 */
@Mapper
public interface CrowdTagDetailMapper extends BaseMapper<CrowdTagDetailPO> {

    /**
     * 检查用户是否在标签内
     * 
     * @param userId 用户ID
     * @param tagId 标签ID
     * @return 存在返回1，不存在返回0
     */
    int checkUserInTag(@Param("userId") String userId, 
                       @Param("tagId") String tagId);

    /**
     * 批量插入标签明细
     * 
     * @param detailList 明细列表
     * @return 插入行数
     */
    int batchInsert(@Param("list") List<CrowdTagDetailPO> detailList);

    /**
     * 根据标签ID查询用户列表
     * 
     * @param tagId 标签ID
     * @return 用户ID列表
     */
    List<String> selectUserIdsByTagId(@Param("tagId") String tagId);

    /**
     * 根据标签ID统计用户数量
     * 
     * @param tagId 标签ID
     * @return 用户数量
     */
    Long countUsersByTagId(@Param("tagId") String tagId);

    /**
     * 根据标签ID删除所有明细
     * 
     * @param tagId 标签ID
     * @return 删除行数
     */
    int deleteByTagId(@Param("tagId") String tagId);

    /**
     * 批量检查用户是否在标签内
     * 
     * @param userIds 用户ID列表
     * @param tagId 标签ID
     * @return 在标签内的用户ID列表
     */
    List<String> batchCheckUsersInTag(@Param("userIds") List<String> userIds,
                                       @Param("tagId") String tagId);
}