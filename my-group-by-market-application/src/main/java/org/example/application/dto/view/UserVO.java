package org.example.application.dto.view;

import lombok.Data;

/**
 * 用户视图对象 (View Object)
 * 职责：仅返回前端需要展示的数据，隐藏密码等敏感字段
 */
@Data
public class UserVO {

    private String id;        // 用户ID (通常转为String防止JS精度丢失)
    private String username;
    private String email;

    // 这里演示了 MapStruct 的 "扁平化映射" 能力
    // Domain层可能是 Address 对象，但这里我们拆开直接给前端
    private String city;
    private String street;
    
    // 注意：绝对没有 password 字段！
}