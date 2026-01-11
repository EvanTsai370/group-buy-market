package org.example.application.service.admin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统信息结果对象
 *
 * <p>
 * Application 层用例输出对象
 *
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统信息")
public class SystemInfoResult {

    @Schema(description = "应用名称")
    private String applicationName;

    @Schema(description = "活动Profile")
    private String activeProfile;

    @Schema(description = "服务端口")
    private int serverPort;

    @Schema(description = "Java版本")
    private String javaVersion;

    @Schema(description = "操作系统名称")
    private String osName;

    @Schema(description = "操作系统版本")
    private String osVersion;

    @Schema(description = "启动时间")
    private LocalDateTime startTime;

    @Schema(description = "最大内存")
    private String maxMemory;

    @Schema(description = "总内存")
    private String totalMemory;

    @Schema(description = "可用内存")
    private String freeMemory;
}
