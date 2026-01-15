package org.example.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.shared.IdGenerator;
import org.example.infrastructure.persistence.converter.AccountConverter;
import org.example.infrastructure.persistence.mapper.AccountMapper;
import org.example.infrastructure.persistence.po.AccountPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Account 仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountMapper accountMapper;
    private final IdGenerator idGenerator;

    @Override
    public void save(Account account) {
        AccountPO po = AccountConverter.INSTANCE.toPO(account);

        // 检查账户是否已存在
        AccountPO existing = accountMapper.selectById(po.getAccountId());

        if (existing == null) {
            // 新账户：使用 insert()
            int rows = accountMapper.insert(po);
            if (rows > 0) {
                log.info("【AccountRepository】创建账户成功, accountId: {}, userId: {}",
                        account.getAccountId(), account.getUserId());
            } else {
                log.warn("【AccountRepository】创建账户失败, accountId: {}, userId: {}",
                        account.getAccountId(), account.getUserId());
            }
        } else {
            // 已存在账户：使用 updateById() 触发乐观锁
            int rows = accountMapper.updateById(po);
            if (rows > 0) {
                log.info("【AccountRepository】更新账户成功, accountId: {}, userId: {}, version: {}",
                        account.getAccountId(), account.getUserId(), po.getVersion());
            } else {
                // 更新失败：可能是乐观锁冲突（version 不匹配）
                log.warn("【AccountRepository】更新账户失败(可能是乐观锁冲突), accountId: {}, userId: {}, version: {}",
                        account.getAccountId(), account.getUserId(), po.getVersion());
                throw new org.springframework.dao.OptimisticLockingFailureException(
                        String.format("Account 更新失败，乐观锁冲突: accountId=%s, version=%s",
                                account.getAccountId(), po.getVersion()));
            }
        }
    }

    @Override
    public Optional<Account> findByUserAndActivity(String userId, String activityId) {
        AccountPO po = accountMapper.selectByUserAndActivity(userId, activityId);
        if (po == null) {
            return Optional.empty();
        }

        Account account = AccountConverter.INSTANCE.toDomain(po);
        return Optional.of(account);
    }

    @Override
    public void batchCreate(List<Account> accounts) {
        for (Account account : accounts) {
            save(account);
        }
        log.info("【AccountRepository】批量创建账户, count: {}", accounts.size());
    }

    @Override
    public String nextId() {
        return "ACC" + idGenerator.nextId();
    }
}