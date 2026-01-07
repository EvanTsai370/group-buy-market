package org.example.infrastructure.persistence.converter;

import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.trade.valueobject.NotifyConfig;
import org.example.domain.model.trade.valueobject.NotifyType;
import org.example.infrastructure.persistence.po.NotificationTaskPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 通知任务转换器
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Mapper(componentModel = "spring")
public interface NotificationTaskConverter {

    /**
     * PO转领域模型
     *
     * @param po 持久化对象
     * @return 领域模型
     */
    @Mapping(target = "notifyConfig", expression = "java(buildNotifyConfig(po))")
    @Mapping(target = "status", expression = "java(org.example.domain.model.notification.valueobject.NotificationStatus.fromCode(po.getStatus()))")
    NotificationTask toDomain(NotificationTaskPO po);

    /**
     * 领域模型转PO
     *
     * @param task 领域模型
     * @return 持久化对象
     */
    @Mapping(target = "notifyType", expression = "java(task.getNotifyConfig() != null ? task.getNotifyConfig().getNotifyType().getCode() : null)")
    @Mapping(target = "notifyUrl", expression = "java(task.getNotifyConfig() != null ? task.getNotifyConfig().getNotifyUrl() : null)")
    @Mapping(target = "notifyMq", expression = "java(task.getNotifyConfig() != null ? task.getNotifyConfig().getNotifyMq() : null)")
    @Mapping(target = "status", expression = "java(task.getStatus().getCode())")
    NotificationTaskPO toPO(NotificationTask task);

    /**
     * 构建NotifyConfig
     *
     * @param po 持久化对象
     * @return 通知配置
     */
    default NotifyConfig buildNotifyConfig(NotificationTaskPO po) {
        if (po.getNotifyType() == null) {
            return null;
        }
        return NotifyConfig.builder()
                .notifyType(NotifyType.fromCode(po.getNotifyType()))
                .notifyUrl(po.getNotifyUrl())
                .notifyMq(po.getNotifyMq())
                .build();
    }
}
