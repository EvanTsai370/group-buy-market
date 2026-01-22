package org.example.interfaces.web.assembler;

import org.example.common.model.PageResult;
import org.example.domain.model.tag.CrowdTag;
import org.example.interfaces.web.dto.admin.TagResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminTagAssembler {

    public TagResponse toResponse(CrowdTag tag) {
        if (tag == null) {
            return null;
        }
        return TagResponse.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .tagRule(tag.getTagRule())
                .status(tag.getStatus() != null ? tag.getStatus().name() : null)
                .userCount(tag.getStatistics())
                .createTime(tag.getCreateTime())
                .updateTime(tag.getUpdateTime())
                .build();
    }

    public List<TagResponse> toResponseList(List<CrowdTag> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PageResult<TagResponse> toPageResponse(PageResult<CrowdTag> page) {
        if (page == null) {
            return new PageResult<>();
        }
        return new PageResult<>(toResponseList(page.getList()), page.getTotal(), page.getPage(), page.getSize());
    }
}
