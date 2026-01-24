package org.example.application.service.admin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 用户统计结果对象
 *
 * <p>
 * Application 层用例输出对象
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户统计")
public class UserStatisticsResult {

    @Schema(description = "用户总数")
    private long totalUsers;

    @Schema(description = "角色分布")
    private Map<String, Long> roleDistribution;
}
