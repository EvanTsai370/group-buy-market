package org.example.interfaces.web.assembler;

import org.example.application.service.payment.result.PaymentQueryResultObj;
import org.example.application.service.payment.result.RefundQueryResultObj;
import org.example.application.service.payment.result.RefundResultObj;
import org.example.interfaces.web.dto.payment.PaymentQueryResponse;
import org.example.interfaces.web.dto.payment.RefundQueryResponse;
import org.example.interfaces.web.dto.payment.RefundResponse;
import org.mapstruct.Mapper;

/**
 * 支付转换器（Interfaces 层）
 *
 * <p>
 * 职责：Result → Response 转换
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Mapper(componentModel = "spring")
public interface PaymentAssembler {

    /**
     * PaymentQueryResultObj → PaymentQueryResponse
     */
    PaymentQueryResponse toResponse(PaymentQueryResultObj result);

    /**
     * RefundResultObj → RefundResponse
     */
    RefundResponse toResponse(RefundResultObj result);

    /**
     * RefundQueryResultObj → RefundQueryResponse
     */
    RefundQueryResponse toResponse(RefundQueryResultObj result);
}
