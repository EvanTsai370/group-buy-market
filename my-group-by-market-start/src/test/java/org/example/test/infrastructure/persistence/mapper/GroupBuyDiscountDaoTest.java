package org.example.test.infrastructure.persistence.mapper;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.infrastructure.persistence.mapper.GroupBuyDiscountMapper;
import org.example.infrastructure.persistence.po.GroupBuyDiscountPO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class GroupBuyDiscountDaoTest {

    @Resource
    private GroupBuyDiscountMapper groupBuyDiscountDao;

    @Test
    public void test_queryGroupBuyDiscountList(){
        List<GroupBuyDiscountPO> groupBuyDiscounts = groupBuyDiscountDao.queryGroupBuyDiscountList();
        log.info("测试结果:{}", JSON.toJSONString(groupBuyDiscounts));
    }

}
