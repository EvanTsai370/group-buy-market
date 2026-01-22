package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.model.PageResult;
import org.example.domain.model.tag.CrowdTag;
import org.example.domain.model.tag.repository.CrowdTagRepository;
import org.example.domain.model.tag.valueobject.TagStatus;
import org.example.domain.service.validation.CrowdTagValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 管理后台人群标签服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final CrowdTagRepository crowdTagRepository;
    private final CrowdTagValidationService validationService;

    /**
     * 分页查询标签列表
     */
    public PageResult<CrowdTag> listTags(int page, int size, String keyword, String status) {
        log.info("【AdminTagService】listTags called with page={}, size={}, keyword={}, status={}", page, size, keyword,
                status);
        try {
            return crowdTagRepository.findByPage(page, size, keyword, status);
        } catch (Exception e) {
            log.error("【AdminTagService】listTags failed", e);
            System.err.println("FATAL: AdminTagService.listTags failed");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 获取标签详情
     */
    public CrowdTag getTag(String tagId) {
        return crowdTagRepository.findById(tagId)
                .orElseThrow(() -> new BizException(String.format("标签不存在: %s", tagId)));
    }

    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTag(String tagName, String tagRule) {
        // 校验规则格式
        if (!validationService.validateRuleFormat(tagRule)) {
            throw new BizException("标签规则格式无效");
        }

        CrowdTag tag = new CrowdTag();
        tag.setTagId(crowdTagRepository.nextId());
        tag.setTagName(tagName);
        tag.setTagRule(tagRule);
        tag.setStatus(TagStatus.DRAFT);
        tag.init();

        crowdTagRepository.save(tag);
        log.info("【AdminTagService】创建标签成功, tagId: {}", tag.getTagId());
    }

    /**
     * 更新标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(String tagId, String tagName, String tagRule) {
        CrowdTag tag = getTag(tagId);

        // 如果包含规则更新，需要重置状态
        boolean ruleChanged = StringUtils.hasText(tagRule) && !tagRule.equals(tag.getTagRule());

        if (StringUtils.hasText(tagName)) {
            tag.setTagName(tagName);
        }
        if (StringUtils.hasText(tagRule)) {
            // 校验规则格式
            if (!validationService.validateRuleFormat(tagRule)) {
                throw new BizException("标签规则格式无效");
            }
            tag.setTagRule(tagRule);
        }

        if (ruleChanged) {
            tag.setStatus(TagStatus.DRAFT);
        }

        tag.update();
        crowdTagRepository.save(tag);
        log.info("【AdminTagService】更新标签成功, tagId: {}", tagId);
    }

    /**
     * 触发标签计算
     */
    public void calculateTag(String tagId) {
        CrowdTag tag = getTag(tagId);

        // 这里只是简单模拟触发计算，实际应该发送消息到MQ或调用计算服务
        // 为了演示，直接更新状态为计算中
        tag.setStatus(TagStatus.CALCULATING);
        crowdTagRepository.save(tag);

        // TODO: 发送异步计算任务
        log.info("【AdminTagService】提交标签计算任务, tagId: {}", tagId);
    }
}
