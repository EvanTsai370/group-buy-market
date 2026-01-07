package org.example.infrastructure.persistence.converter;

import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.NotifyConfig;
import org.example.domain.model.trade.valueobject.NotifyStatus;
import org.example.domain.model.trade.valueobject.NotifyType;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.infrastructure.persistence.po.TradeOrderPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * TradeOrder转换器
 *
 * <p>职责：Domain对象 ↔ PO对象 的转换
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Mapper(componentModel = "spring")
public interface TradeOrderConverter {

    /**
     * Domain对象 → PO对象
     *
     * @param tradeOrder 领域对象
     * @return PO对象
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "tradeStatusToCode")
    @Mapping(source = "notifyConfig.notifyType", target = "notifyType", qualifiedByName = "notifyTypeToCode")
    @Mapping(source = "notifyConfig.notifyUrl", target = "notifyUrl")
    @Mapping(source = "notifyConfig.notifyMq", target = "notifyMq")
    @Mapping(source = "notifyConfig.notifyStatus", target = "notifyStatus", qualifiedByName = "notifyStatusToCode")
    TradeOrderPO toPO(TradeOrder tradeOrder);

    /**
     * PO对象 → Domain对象
     *
     * @param po PO对象
     * @return 领域对象
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "codeToTradeStatus")
    @Mapping(source = ".", target = "notifyConfig", qualifiedByName = "poToNotifyConfig")
    @Mapping(target = "domainEvents", ignore = true)
    TradeOrder toDomain(TradeOrderPO po);

    // ==================== 自定义映射方法 ====================

    /**
     * TradeStatus枚举 → 数据库code
     */
    @Named("tradeStatusToCode")
    default String tradeStatusToCode(TradeStatus status) {
        return status != null ? status.getCode() : null;
    }

    /**
     * 数据库code → TradeStatus枚举
     */
    @Named("codeToTradeStatus")
    default TradeStatus codeToTradeStatus(String code) {
        return TradeStatus.fromCode(code);
    }

    /**
     * NotifyType枚举 → 数据库code
     */
    @Named("notifyTypeToCode")
    default String notifyTypeToCode(NotifyType type) {
        return type != null ? type.getCode() : null;
    }

    /**
     * 数据库code → NotifyType枚举
     */
    @Named("codeToNotifyType")
    default NotifyType codeToNotifyType(String code) {
        return NotifyType.fromCode(code);
    }

    /**
     * NotifyStatus枚举 → 数据库code
     */
    @Named("notifyStatusToCode")
    default String notifyStatusToCode(NotifyStatus status) {
        return status != null ? status.getCode() : null;
    }

    /**
     * 数据库code → NotifyStatus枚举
     */
    @Named("codeToNotifyStatus")
    default NotifyStatus codeToNotifyStatus(String code) {
        return NotifyStatus.fromCode(code);
    }

    /**
     * PO对象 → NotifyConfig值对象
     */
    @Named("poToNotifyConfig")
    default NotifyConfig poToNotifyConfig(TradeOrderPO po) {
        if (po.getNotifyType() == null) {
            return null;
        }

        return NotifyConfig.builder()
                .notifyType(codeToNotifyType(po.getNotifyType()))
                .notifyUrl(po.getNotifyUrl())
                .notifyMq(po.getNotifyMq())
                .notifyStatus(codeToNotifyStatus(po.getNotifyStatus()))
                .build();
    }
}
