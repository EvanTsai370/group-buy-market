package org.example.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.example.infrastructure.persistence.converter.UserConverter;
import org.example.infrastructure.persistence.mapper.UserMapper;
import org.example.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Repository;

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
        userMapper.insert(po);
    }

    @Override
    public void update(User user) {
        UserPO existing = userMapper.selectByUserId(user.getUserId());
        if (existing != null) {
            UserPO po = userConverter.toPO(user);
            po.setId(existing.getId());
            userMapper.updateById(po);
        }
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
}
