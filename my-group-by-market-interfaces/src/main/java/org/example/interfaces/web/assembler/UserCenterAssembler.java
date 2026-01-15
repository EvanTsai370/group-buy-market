package org.example.interfaces.web.assembler;

import org.example.application.service.customer.result.UserOrderResult;
import org.example.application.service.customer.result.UserProfileResult;
import org.example.interfaces.web.dto.customer.UserOrderResponse;
import org.example.interfaces.web.dto.customer.UserProfileResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 用户中心 Assembler（MapStruct）
 * 负责 Application Result → Interfaces Response 转换
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Mapper(componentModel = "spring")
public interface UserCenterAssembler {

    /**
     * 用户资料结果 → 响应
     */
    UserProfileResponse toUserProfileResponse(UserProfileResult result);

    /**
     * 用户订单结果 → 响应
     */
    UserOrderResponse toUserOrderResponse(UserOrderResult result);

    /**
     * 用户订单结果列表 → 响应列表
     */
    List<UserOrderResponse> toOrderListResponse(List<UserOrderResult> results);
}
