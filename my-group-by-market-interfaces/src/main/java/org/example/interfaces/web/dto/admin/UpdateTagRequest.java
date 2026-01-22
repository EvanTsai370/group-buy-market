package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新标签请求
 */
@Data
@Schema(description = "更新标签请求")
public class UpdateTagRequest {
    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签规则(JSON)")
    private String tagRule;
}
