package org.example.domain.shared;

/**
 * ID 生成器接口
 * Domain 层只定义“我们需要生成 ID”这件事，不关心具体怎么生成
 */
public interface IdGenerator {
    /**
     * 生成下一个唯一 ID
     */
    Long nextId();
}