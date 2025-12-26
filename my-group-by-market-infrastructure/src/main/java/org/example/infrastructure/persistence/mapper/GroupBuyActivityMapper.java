package org.example.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.persistence.po.GroupBuyActivityPO;

import java.util.List;

@Mapper
public interface GroupBuyActivityMapper {

    List<GroupBuyActivityPO> queryGroupBuyActivityList();

}
