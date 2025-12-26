package org.example.application.service;

import org.example.application.assembler.UserAssembler;
import org.example.application.dto.cmd.UserRegisterCmd;
import org.example.application.dto.view.UserVO;
import org.example.domain.model.user.User;
import org.example.domain.model.user.UserRepository;
import org.example.domain.model.user.event.UserCreatedEvent;
import org.example.domain.shared.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 单元测试：UserApplicationService
 * 使用 @ExtendWith(MockitoExtension.class) 启用 Mockito，不启动 Spring Context (速度极快)
 */
@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    // 1. 定义“假”依赖 (@Mock)
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAssembler userAssembler;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    // 2. 注入“真”服务 (@InjectMocks)
    // Mockito 会自动把上面 4 个假对象注入到这个 service 实例中
    @InjectMocks
    private UserApplicationService userApplicationService;

    @Test
    @DisplayName("注册成功场景：生成ID -> 发事件 -> 保存 -> 返回VO")
    void should_register_user_successfully() {
        // --- Given (准备数据和行为) ---
        
        // 1. 准备入参
        UserRegisterCmd cmd = new UserRegisterCmd();
        cmd.setUsername("batman");
        cmd.setEmail("b@gotham.com");

        // 2. 准备过程中的对象
        User userEntity = new User("batman"); // 此时 id 为空
        UserVO expectedVO = new UserVO();
        expectedVO.setId("10086");
        expectedVO.setUsername("batman");

        // 3. 告诉 Mock 对象该怎么演戏 (Stubbing)
        // 当调用 assembler.toEntity 时，返回 userEntity
        when(userAssembler.toEntity(cmd)).thenReturn(userEntity);
        // 当调用 idGenerator 时，返回 10086L
        when(idGenerator.nextId()).thenReturn(10086L);
        // 当调用 repository.save 时，返回 userEntity (模拟保存成功)
        when(userRepository.save(any(User.class))).thenReturn(userEntity);
        // 当调用 assembler.toVO 时，返回 expectedVO
        when(userAssembler.toVO(any(User.class))).thenReturn(expectedVO);

        // --- When (执行测试) ---
        UserVO actualVO = userApplicationService.registerUser(cmd);

        // --- Then (验证结果) ---
        
        // 1. 验证返回值是否符合预期
        assertEquals("10086", actualVO.getId());
        assertEquals("batman", actualVO.getUsername());
        
        // 2. [关键] 验证核心流程是否被执行了
        // 验证 idGenerator 是否被调用了 1 次
        verify(idGenerator, times(1)).nextId();
        // 验证是否发布了事件 (any() 表示不关心参数具体内容，只关心发没发)
        verify(eventPublisher, times(1)).publishEvent(any(UserCreatedEvent.class));
        // 验证是否调用了保存
        verify(userRepository, times(1)).save(userEntity);
    }
}