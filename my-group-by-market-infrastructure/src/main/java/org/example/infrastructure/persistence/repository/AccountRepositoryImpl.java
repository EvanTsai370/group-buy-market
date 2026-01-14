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

        // MyBatis-Plus 会根据主键是否存在自动判断 INSERT/UPDATE
        boolean success = accountMapper.insertOrUpdate(po);
        if (success) {
            log.info("【AccountRepository】保存账户成功, accountId: {}, userId: {}",
                    account.getAccountId(), account.getUserId());
        } else {
            log.warn("【AccountRepository】保存账户失败, accountId: {}, userId: {}",
                    account.getAccountId(), account.getUserId());
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