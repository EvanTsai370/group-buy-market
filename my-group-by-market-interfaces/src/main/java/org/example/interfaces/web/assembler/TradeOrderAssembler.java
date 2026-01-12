package org.example.interfaces.web.assembler;

import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.result.TradeOrderResult;
import org.example.interfaces.web.dto.LockOrderRequest;
import org.example.interfaces.web.dto.TradeOrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 交易订单转换器（Interfaces 层）
 *
 * <p>
 * 职责：协议层与应用层的转换
 * <ul>
 * <li>Request → Command：将 HTTP 请求转换为用例命令</li>
 * <li>Result → Response：将用例结果转换为 HTTP 响应</li>
 * </ul>
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>这是 Interfaces 层的 Assembler，负责协议模型与用例模型的转换</li>
 * <li>与 Application 层的 TradeOrderResultAssembler 职责不同</li>
 * <li>确保层次边界清晰，避免协议层污染业务层</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Mapper(componentModel = "spring")
public interface TradeOrderAssembler {

    /**
     * Request → Command 转换（使用认证上下文的 userId）
     *
     * @param request 锁单请求
     * @param userId  当前认证用户ID（从 SecurityContextUtils 获取）
     * @return 锁单命令
     */
    @Mapping(target = "userId", source = "userId")
    LockOrderCmd toCommand(LockOrderRequest request, String userId);

    /**
     * Result → Response 转换
     *
     * @param result 交易订单结果
     * @return 交易订单响应
     */
    TradeOrderResponse toResponse(TradeOrderResult result);
}
