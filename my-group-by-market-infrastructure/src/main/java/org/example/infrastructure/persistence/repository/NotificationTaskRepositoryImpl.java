package org.example.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.infrastructure.persistence.converter.NotificationTaskConverter;
import org.example.infrastructure.persistence.mapper.NotificationTaskMapper;
import org.example.infrastructure.persistence.po.NotificationTaskPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通知任务仓储实现
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationTaskRepositoryImpl implements NotificationTaskRepository {

    private final NotificationTaskMapper notificationTaskMapper;
    private final NotificationTaskConverter notificationTaskConverter;

    @Override
    public void save(NotificationTask task) {
        NotificationTaskPO po = notificationTaskConverter.toPO(task);
        notificationTaskMapper.insert(po);
        log.debug("保存通知任务: taskId={}, tradeOrderId={}", task.getTaskId(), task.getTradeOrderId());
    }

    @Override
    public void update(NotificationTask task) {
        NotificationTaskPO po = notificationTaskConverter.toPO(task);
        notificationTaskMapper.updateById(po);
        log.debug("更新通知任务: taskId={}, status={}", task.getTaskId(), task.getStatus());
    }

    @Override
    public Optional<NotificationTask> findByTaskId(String taskId) {
        NotificationTaskPO po = notificationTaskMapper.selectById(taskId);
        return Optional.ofNullable(po).map(notificationTaskConverter::toDomain);
    }

    @Override
    public List<NotificationTask> findByTradeOrderId(String tradeOrderId) {
        List<NotificationTaskPO> pos = notificationTaskMapper.selectByTradeOrderId(tradeOrderId);
        return pos.stream()
                .map(notificationTaskConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTask> findPendingTasks(int limit) {
        List<NotificationTaskPO> pos = notificationTaskMapper.selectPendingTasks(limit);
        return pos.stream()
                .map(notificationTaskConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTask> findPendingTasks(int offset, int limit) {
        List<NotificationTaskPO> pos = notificationTaskMapper.selectPendingTasksWithPage(offset, limit);
        return pos.stream()
                .map(notificationTaskConverter::toDomain)
                .collect(Collectors.toList());
    }
}
