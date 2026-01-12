package org.example.application.service.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.customer.result.UserOrderResult;
import org.example.application.service.customer.result.UserProfileResult;
import org.example.common.exception.BizException;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户中心服务
 * 
 * 提供给前端用户的个人中心相关接口，包括：
 * - 用户资料
 * - 用户订单列表
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCenterService {

    private final UserRepository userRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final ActivityRepository activityRepository;
    private final SkuRepository skuRepository;

    /**
     * 获取用户资料
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    public UserProfileResult getUserProfile(String userId) {
        log.info("【UserCenterService】查询用户资料，userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        UserProfileResult result = new UserProfileResult();
        result.setUserId(user.getUserId());
        result.setUsername(user.getUsername());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setPhone(maskPhone(user.getPhone()));
        result.setEmail(maskEmail(user.getEmail()));
        result.setLastLoginTime(user.getLastLoginTime());
        result.setCreateTime(user.getCreateTime());

        log.info("【UserCenterService】查询用户资料完成，userId: {}", userId);
        return result;
    }

    /**
     * 获取用户订单列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 订单列表
     */
    public List<UserOrderResult> getUserOrders(String userId, int page, int size) {
        log.info("【UserCenterService】查询用户订单，userId: {}, page: {}, size: {}", userId, page, size);

        List<TradeOrder> tradeOrders = tradeOrderRepository.findByUserId(userId, page, size);
        List<UserOrderResult> results = new ArrayList<>();

        for (TradeOrder tradeOrder : tradeOrders) {
            UserOrderResult result = new UserOrderResult();
            result.setTradeOrderId(tradeOrder.getTradeOrderId());
            result.setOutTradeNo(tradeOrder.getOutTradeNo());
            result.setSkuId(tradeOrder.getSkuId());
            result.setActivityId(tradeOrder.getActivityId());
            result.setOrderId(tradeOrder.getOrderId());
            result.setTradeAmount(tradeOrder.getPayPrice());
            result.setTradeStatus(tradeOrder.getStatus() != null ? tradeOrder.getStatus().name() : null);
            result.setCreateTime(tradeOrder.getCreateTime());
            result.setPayTime(tradeOrder.getPayTime());

            // 查询商品名称
            skuRepository.findBySkuId(tradeOrder.getSkuId())
                    .ifPresent(sku -> result.setGoodsName(sku.getGoodsName()));

            // 查询活动名称
            activityRepository.findById(tradeOrder.getActivityId())
                    .ifPresent(activity -> result.setActivityName(activity.getActivityName()));

            results.add(result);
        }

        log.info("【UserCenterService】查询用户订单完成，userId: {}, count: {}", userId, results.size());
        return results;
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
