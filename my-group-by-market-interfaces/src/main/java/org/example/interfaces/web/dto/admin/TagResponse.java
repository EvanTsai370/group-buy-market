package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "标签详情响应")
public class TagResponse {
    @Schema(description = "标签ID")
    private String tagId;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签规则")
    private String tagRule;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "用户数量")
    private Long userCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
