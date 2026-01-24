package org.example.domain.model.trade.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.chain.model2.IChainHandler;
import org.example.domain.service.validation.FlowControlService;

/**
 * 流控校验过滤器
 * 
 * <p>
 * 职责：
 * <ul>
 * <li>检查系统降级开关</li>
 * <li>检查用户是否在切量灰度范围内</li>
 * </ul>
 * 
 * <p>
 * 执行顺序：第一个执行（最早拦截）
 * 
 * <p>
 * 设计理念：早失败原则 - 在最前面拦截不符合条件的请求，避免浪费系统资源
 * 
 */
@Slf4j
public class FlowControlHandler implements IChainHandler<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> {

    private final FlowControlService flowControlService;

    public FlowControlHandler(FlowControlService flowControlService) {
        this.flowControlService = flowControlService;
    }

    @Override
    public TradeFilterResponse handle(TradeFilterRequest request, TradeFilterContext context) throws Exception {
        log.info("【流控校验过滤器】开始执行，userId: {}", request.getUserId());

        // 调用流控服务校验（会抛出 BizException）
        flowControlService.validateFlowControl(request.getUserId());

        log.info("【流控校验过滤器】校验通过，userId: {}", request.getUserId());

        // 返回允许继续执行
        return TradeFilterResponse.allow();
    }
}
