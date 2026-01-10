package org.example.interfaces.web.assembler;

import org.example.application.service.admin.result.*;
import org.example.interfaces.web.dto.admin.*;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 管理后台转换器（Interfaces 层）
 *
 * <p>
 * 职责：应用层结果 → 协议层响应转换
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Mapper(componentModel = "spring")
public interface AdminDashboardAssembler {

    /**
     * DashboardOverviewResult → DashboardOverviewResponse
     */
    DashboardOverviewResponse toResponse(DashboardOverviewResult result);

    /**
     * UserStatisticsResult → UserStatisticsResponse
     */
    UserStatisticsResponse toResponse(UserStatisticsResult result);

    /**
     * GoodsStatisticsResult → GoodsStatisticsResponse
     */
    GoodsStatisticsResponse toResponse(GoodsStatisticsResult result);

    /**
     * SkuStatisticsInfo → SkuStatisticsInfoResponse
     */
    SkuStatisticsInfoResponse toResponse(SkuStatisticsInfo info);

    /**
     * SkuStatisticsInfo 列表转换
     */
    List<SkuStatisticsInfoResponse> toSkuResponseList(List<SkuStatisticsInfo> infos);

    /**
     * UserDetailResult → UserDetailResponse
     */
    UserDetailResponse toResponse(UserDetailResult result);

    /**
     * UserDetailResult 列表转换
     */
    List<UserDetailResponse> toUserResponseList(List<UserDetailResult> results);
}
