package org.example.test.infrastructure.persistence.mapper;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.persistence.mapper.GroupBuyActivityMapper;
import org.example.infrastructure.persistence.po.GroupBuyActivityPO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class GroupBuyActivityDaoTest {

    @Resource
    private GroupBuyActivityMapper groupBuyActivityDao;

    @Test
    void test_queryGroupBuyActivityList() {
        List<GroupBuyActivityPO> list =
                groupBuyActivityDao.queryGroupBuyActivityList();
        log.info("测试结果:{}", JSON.toJSONString(list));
    }
}
