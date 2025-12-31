// ============ 文件: AccountConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.account.Account;
import org.example.infrastructure.persistence.po.AccountPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Account 转换器
 */
@Mapper
public interface AccountConverter {

    AccountConverter INSTANCE = Mappers.getMapper(AccountConverter.class);

    /**
     * PO 转 Domain Entity
     */
    Account toDomain(AccountPO po);

    /**
     * Domain Entity 转 PO
     */
    AccountPO toPO(Account account);
}