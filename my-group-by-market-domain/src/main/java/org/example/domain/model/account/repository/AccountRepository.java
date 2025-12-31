package org.example.domain.model.account.repository;

import org.example.domain.model.account.Account;

import java.util.List;
import java.util.Optional;

/**
 * Account 仓储接口
 */
public interface AccountRepository {

    /**
     * 保存账户
     *
     * @param account 账户聚合
     */
    void save(Account account);

    /**
     * 根据用户和活动查找账户
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 账户聚合
     */
    Optional<Account> findByUserAndActivity(String userId, String activityId);

    /**
     * 批量创建账户
     *
     * @param accounts 账户列表
     */
    void batchCreate(List<Account> accounts);

    /**
     * 生成下一个账户ID
     *
     * @return 账户ID
     */
    String nextId();
}