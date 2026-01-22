package org.example.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.example.infrastructure.persistence.converter.UserConverter;
import org.example.infrastructure.persistence.mapper.UserMapper;
import org.example.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public void save(User user) {
        UserPO po = userConverter.toPO(user);
        // MyBatis-Plus 会根据主键是否存在自动判断 INSERT/UPDATE
        userMapper.insertOrUpdate(po);
    }

    @Override
    public void update(User user) {
        UserPO po = userConverter.toPO(user);
        // 直接使用业务ID更新
        userMapper.updateById(po);
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        UserPO po = userMapper.selectByUserId(userId);
        return Optional.ofNullable(po).map(userConverter::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserPO po = userMapper.selectByUsername(username);
        return Optional.ofNullable(po).map(userConverter::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        UserPO po = userMapper.selectByPhone(phone);
        return Optional.ofNullable(po).map(userConverter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserPO po = userMapper.selectByEmail(email);
        return Optional.ofNullable(po).map(userConverter::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userMapper.existsByPhone(phone);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    @Override
    public long count() {
        return userMapper.selectCount(null);
    }

    @Override
    public List<User> findAll(int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserPO> pageResult = userMapper
                .selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), null);
        return pageResult.getRecords().stream()
                .map(userConverter::toDomain)
                .toList();
    }

    @Override
    public long countByCreateTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserPO> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.ge(UserPO::getCreateTime, start);
        wrapper.le(UserPO::getCreateTime, end);
        return userMapper.selectCount(wrapper);
    }

    @Override
    public org.example.common.model.PageResult<User> findByPage(int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserPO> pageResult = userMapper
                .selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), null);
        List<User> list = pageResult.getRecords().stream()
                .map(userConverter::toDomain)
                .toList();
        return new org.example.common.model.PageResult<>(list, pageResult.getTotal(), page, size);
    }
}
