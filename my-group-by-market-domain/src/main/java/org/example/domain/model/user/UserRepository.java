package org.example.domain.model.user;

// 这是个接口！不要依赖 MyBatis！
public interface UserRepository {
    User save(User user);
}