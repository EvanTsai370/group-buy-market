package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建标签请求
 */
@Data
@Schema(description = "创建标签请求")
public class CreateTagRequest {
    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", example = "高价值用户")
    private String tagName;

    @NotBlank(message = "标签规则不能为空")
    @Schema(description = "标签规则(JSON)", example = "{\"age\": {\"$gte\": 18}}")
    private String tagRule;
}
