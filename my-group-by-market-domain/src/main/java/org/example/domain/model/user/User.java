package org.example.domain.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 【必须】MyBatis, Jackson, MapStruct 反射都需要无参构造
@AllArgsConstructor // 【可选】测试时方便全量构造
public class User {
    private Long id; // 领域唯一标识
    private String name;
    private String password; // 可能是加密后的
    private String email;
    private Address address; // 引用值对象

    // 这符合 DDD 的理念：对象一出生就应该具备核心属性
    public User(String name) {
        this.name = name;
    }

    // 或者更严谨的构造器，要求创建时必须有 name 和 email
    /*
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    */

    // 充血模型行为
    public void register() {
        // 可以在这里写业务逻辑，例如生成默认昵称、校验状态等
        System.out.println("Domain Logic: User is registering...");
    }
}