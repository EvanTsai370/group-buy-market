// ============ 文件: application/service/impl/TrialServiceImpl.java ============
package org.example.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.tag.repository.CrowdTagRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialServiceImpl {

    private final CrowdTagRepository crowdTagRepository;

    /**
     * 校验用户是否有资格参与活动
     */
    public boolean validateUserEligibility(String userId, String tagId) {
        // 调用仓储方法（自动走缓存）
        Boolean inTag = crowdTagRepository.checkUserInTag(userId, tagId);
        
        if (!inTag) {
            log.warn("【试算服务】用户不在人群标签内, userId: {}, tagId: {}", userId, tagId);
            return false;
        }
        
        log.info("【试算服务】用户资格校验通过, userId: {}, tagId: {}", userId, tagId);
        return true;
    }
}