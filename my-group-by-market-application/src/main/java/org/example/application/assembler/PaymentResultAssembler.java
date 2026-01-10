package org.example.application.assembler;

import org.example.application.service.payment.result.PaymentQueryResultObj;
import org.example.application.service.payment.result.RefundQueryResultObj;
import org.example.application.service.payment.result.RefundResultObj;
import org.example.domain.gateway.PaymentGateway;
import org.mapstruct.Mapper;

/**
 * 支付结果转换器（Application 层）
 *
 * <p>
 * 职责：Gateway record → Application Result 转换
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Mapper(componentModel = "spring")
public interface PaymentResultAssembler {

    /**
     * PaymentQueryResult → PaymentQueryResultObj 转换
     */
    PaymentQueryResultObj toResult(PaymentGateway.PaymentQueryResult gatewayResult);

    /**
     * RefundResult → RefundResultObj 转换
     */
    RefundResultObj toResult(PaymentGateway.RefundResult gatewayResult);

    /**
     * RefundQueryResult → RefundQueryResultObj 转换
     */
    RefundQueryResultObj toResult(PaymentGateway.RefundQueryResult gatewayResult);
}
