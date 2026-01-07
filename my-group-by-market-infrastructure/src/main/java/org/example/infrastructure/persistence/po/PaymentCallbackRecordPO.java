package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调记录持久化对象
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Data
@TableName("payment_callback_record")
public class PaymentCallbackRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordId;

    private String callbackId;

    private String tradeOrderId;

    private BigDecimal amount;

    private LocalDateTime payTime;

    private String channel;

    private String paymentNo;

    private LocalDateTime createTime;
}
