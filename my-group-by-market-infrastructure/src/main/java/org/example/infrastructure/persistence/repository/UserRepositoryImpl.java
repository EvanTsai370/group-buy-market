package org.example.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.example.domain.model.user.User;
import org.example.domain.model.user.UserRepository;
import org.example.infrastructure.persistence.converter.UserDataConverter;
import org.example.infrastructure.persistence.mapper.UserMapper;
import org.example.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper; // MyBatis-Plus 的 Mapper
    private final UserDataConverter converter; // MapStruct 转换器

    @Override
    public User save(User user) {
        // [关键] 1. Domain Entity -> PO (准备入库)
        UserPO po = converter.toPO(user);

        // 2. 调用 MyBatis 进行数据库操作
        userMapper.insert(po);

        // 3. 回填 ID (MyBatis 插入后会回填 ID 到 PO)
        user.setId(po.getId());

        // 4. 返回 Domain Entity (通常可以直接返回传入的 user，或者重新转一次)
        return user;
    }
}