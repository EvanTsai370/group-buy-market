package org.example.application.assembler;

import org.example.application.service.admin.result.SkuStatisticsInfo;
import org.example.application.service.admin.result.UserDetailResult;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 用户结果转换器（Application 层）
 *
 * <p>
 * 职责：领域模型 → 视图模型转换
 *
 */
@Mapper(componentModel = "spring")
public interface UserResultAssembler {

    /**
     * User → UserDetailResult 转换
     *
     * @param user 领域对象
     * @return 结果对象
     */
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    UserDetailResult toResult(User user);

    /**
     * User 列表转换
     */
    List<UserDetailResult> toResultList(List<User> users);

    /**
     * Sku → SkuStatisticsInfo 转换
     */
    SkuStatisticsInfo toSkuStatisticsInfo(Sku sku);

    /**
     * Sku 列表转换
     */
    List<SkuStatisticsInfo> toSkuStatisticsInfoList(List<Sku> skus);
}
