package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.persistence.po.GroupBuyDiscountPO;

import java.util.List;

@Mapper
public interface GroupBuyDiscountMapper {

    List<GroupBuyDiscountPO> queryGroupBuyDiscountList();

}
