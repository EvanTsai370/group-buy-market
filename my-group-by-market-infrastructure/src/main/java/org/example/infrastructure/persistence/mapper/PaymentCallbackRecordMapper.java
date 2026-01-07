package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.persistence.po.PaymentCallbackRecordPO;

/**
 * 支付回调记录Mapper
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Mapper
public interface PaymentCallbackRecordMapper extends BaseMapper<PaymentCallbackRecordPO> {
}
