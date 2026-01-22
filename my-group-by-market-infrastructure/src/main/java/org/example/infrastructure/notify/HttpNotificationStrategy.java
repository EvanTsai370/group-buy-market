package org.example.infrastructure.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.trade.valueobject.NotifyType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP通知策略
 *
 * <p>
 * 通过HTTP POST方式回调业务方接口
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
@Component
public class HttpNotificationStrategy implements CallbackNotificationStrategy {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpNotificationStrategy() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void execute(NotificationTask task) throws Exception {
        String notifyUrl = task.getNotifyConfig().getNotifyUrl();
        if (notifyUrl == null || notifyUrl.isEmpty()) {
            throw new IllegalArgumentException("HTTP通知地址不能为空");
        }

        log.info("【HTTP通知】开始执行, taskId={}, url={}", task.getTaskId(), notifyUrl);

        // 构建回调数据
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("taskId", task.getTaskId());
        requestBody.put("tradeOrderId", task.getTradeOrderId());
        requestBody.put("timestamp", System.currentTimeMillis());

        // 转换为JSON字符串
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(notifyUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            // 执行HTTP请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 检查响应状态
            if (response.statusCode() != 200) {
                throw new Exception("HTTP回调失败, status=" + response.statusCode() + ", body=" + response.body());
            }

            log.info("【HTTP通知】执行成功, taskId={}, response={}", task.getTaskId(), response.body());

        } catch (Exception e) {
            log.error("【HTTP通知】执行失败, taskId={}, url={}", task.getTaskId(), notifyUrl, e);
            throw new Exception("HTTP回调失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(NotificationTask task) {
        return task.getNotifyConfig() != null
                && task.getNotifyConfig().getNotifyType() == NotifyType.HTTP;
    }
}
