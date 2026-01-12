package org.example.interfaces.web.assembler;

import org.example.application.service.admin.cmd.CreateAdminCmd;
import org.example.application.service.admin.result.UserDetailResult;
import org.example.interfaces.web.dto.admin.CreateAdminRequest;
import org.example.interfaces.web.dto.admin.UserDetailResponse;
import org.mapstruct.Mapper;

/**
 * 管理员用户 Assembler
 * 
 * <p>
 * 协议模型 → 用例模型转换
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-12
 */
@Mapper(componentModel = "spring")
public interface AdminUserAssembler {

    /**
     * 请求对象 → 命令对象
     */
    CreateAdminCmd toCommand(CreateAdminRequest request);

    /**
     * 结果对象 → 响应对象
     */
    UserDetailResponse toResponse(UserDetailResult result);
}
