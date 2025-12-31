package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.shared.IdGenerator;
import org.example.infrastructure.persistence.converter.ActivityConverter;
import org.example.infrastructure.persistence.mapper.ActivityMapper;
import org.example.infrastructure.persistence.po.ActivityPO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Activity 仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepository {

    private final ActivityMapper activityMapper;
    private final IdGenerator idGenerator;

    @Override
    public void save(Activity activity) {
        ActivityPO po = ActivityConverter.INSTANCE.toPO(activity);

        if (po.getId() == null) {
            // 新增
            activityMapper.insert(po);
            log.info("【ActivityRepository】新增活动, activityId: {}", activity.getActivityId());
        } else {
            // 更新
            activityMapper.updateById(po);
            log.info("【ActivityRepository】更新活动, activityId: {}", activity.getActivityId());
        }
    }

    @Override
    public Optional<Activity> findById(String activityId) {
        LambdaQueryWrapper<ActivityPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityPO::getActivityId, activityId);

        ActivityPO po = activityMapper.selectOne(wrapper);
        if (po == null) {
            return Optional.empty();
        }

        Activity activity = ActivityConverter.INSTANCE.toDomain(po);
        return Optional.of(activity);
    }

    @Override
    public Optional<Activity> findBySourceAndChannel(String source, String channel) {
        ActivityPO po = activityMapper.selectBySourceAndChannel(source, channel);
        if (po == null) {
            return Optional.empty();
        }

        Activity activity = ActivityConverter.INSTANCE.toDomain(po);
        return Optional.of(activity);
    }

    @Override
    public String nextId() {
        return "ACT" + idGenerator.nextId();
    }
}