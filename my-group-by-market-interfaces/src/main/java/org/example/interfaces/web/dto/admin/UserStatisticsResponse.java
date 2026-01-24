package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 用户统计响应
 *
 * <p>
 * Interfaces 层协议出参
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户统计响应")
public class UserStatisticsResponse {

    @Schema(description = "用户总数")
    private long totalUsers;

    @Schema(description = "角色分布")
    private Map<String, Long> roleDistribution;
}
