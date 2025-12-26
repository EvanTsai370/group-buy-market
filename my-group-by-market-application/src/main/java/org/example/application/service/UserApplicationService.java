package org.example.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.assembler.UserAssembler;
import org.example.application.dto.cmd.UserRegisterCmd;
import org.example.application.dto.view.UserVO;
import org.example.domain.model.user.User;
import org.example.domain.model.user.UserRepository;
import org.example.domain.model.user.event.UserCreatedEvent;
import org.example.domain.shared.IdGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserAssembler userAssembler;
    private final ApplicationEventPublisher eventPublisher;
    private final IdGenerator idGenerator;

    @Transactional // 【关键】开启事务，确保事件在事务提交后（AFTER_COMMIT）才被监听器处理
    public UserVO registerUser(UserRegisterCmd cmd) {
        log.info("开始注册用户, username: {}", cmd.getUsername());

        // 1. DTO 转 Domain Entity
        User user = userAssembler.toEntity(cmd);

        // 【新增】2. 模拟雪花算法生成 ID (硬编码)
        // 在真实 DDD 场景中，这里通常调用 IdGenerator.nextId()
        // 这样做的好处是：User 对象在入库前就是“完整”的，不依赖数据库自增
        user.setId(idGenerator.nextId());

        // 3. 执行业务逻辑 (Domain层)
        user.register();

        // 【新增】4. 发布领域事件
        // 注意：此时 ID 已经生成，可以传给事件对象。
        // 虽然代码执行到这了，但监听器不会立刻跑，而是等下面的事务提交成功后才跑。
        eventPublisher.publishEvent(new UserCreatedEvent(user.getId(), user.getEmail()));

        // 5. 调用 Domain 接口进行保存
        // 因为我们已经预生成了 ID，Infrastructure 层的 Repository 只要把这个 ID 存进去即可
        User savedUser = userRepository.save(user);

        // 6. Domain Entity 转 View Object (返回给前端)
        return userAssembler.toVO(savedUser);
    }
}