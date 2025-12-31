package org.example.common.pattern.flow;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 带异步数据加载的抽象流程节点
 * 在节点执行前并行加载所需数据
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 流程上下文类型
 * @param <RESPONSE> 响应结果类型
 */
@Setter
@Slf4j
public abstract class AbstractAsyncDataFlowNode<REQUEST, CONTEXT, RESPONSE> 
        extends AbstractFlowNode<REQUEST, CONTEXT, RESPONSE> {

    /**
     * 线程池（子类需要注入）
     * -- SETTER --
     *  设置线程池
     *
     * @param executorService 线程池

     */
    protected ExecutorService executorService;

    /**
     * 数据加载超时时间（秒）
     * -- SETTER --
     *  设置数据加载超时时间
     *
     * @param seconds 超时时间（秒）

     */
    protected long dataLoadTimeoutSeconds = 5L;

    @Override
    public RESPONSE execute(REQUEST request, CONTEXT context) {
        log.info("【异步数据节点】开始执行节点: {}", getNodeName());

        try {
            // 1. 异步加载数据
            List<DataLoader<REQUEST, CONTEXT>> loaders = getDataLoaders();
            if (loaders != null && !loaders.isEmpty()) {
                loadDataAsync(request, context, loaders);
            }

            // 2. 执行节点业务逻辑
            RESPONSE response = doExecute(request, context);

            // 3. 路由到下一个节点
            FlowNode<REQUEST, CONTEXT, RESPONSE> nextNode = route(request, context);
            if (nextNode != null) {
                return nextNode.execute(request, context);
            }

            return response;

        } catch (Exception e) {
            log.error("【异步数据节点】节点执行异常: {}", getNodeName(), e);
            return handleException(request, context, e);
        }
    }

    /**
     * 异步加载数据
     *
     * @param request 请求参数
     * @param context 流程上下文
     * @param loaders 数据加载器列表
     */
    private void loadDataAsync(REQUEST request, CONTEXT context, List<DataLoader<REQUEST, CONTEXT>> loaders) {
        log.info("【异步数据节点】开始加载数据，加载器数量: {}", loaders.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (DataLoader<REQUEST, CONTEXT> loader : loaders) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.info("【异步数据节点】执行数据加载器: {}", loader.getLoaderName());
                    loader.loadData(request, context);
                } catch (Exception e) {
                    log.error("【异步数据节点】数据加载器执行失败: {}", loader.getLoaderName(), e);
                    throw new RuntimeException("数据加载失败: " + loader.getLoaderName(), e);
                }
            }, executorService);

            futures.add(future);
        }

        // 等待所有数据加载完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(dataLoadTimeoutSeconds, TimeUnit.SECONDS);
            log.info("【异步数据节点】数据加载完成");
        } catch (TimeoutException e) {
            log.error("【异步数据节点】数据加载超时: {}秒", dataLoadTimeoutSeconds);
            throw new RuntimeException("数据加载超时", e);
        } catch (Exception e) {
            log.error("【异步数据节点】数据加载异常", e);
            throw new RuntimeException("数据加载异常", e);
        }
    }

    /**
     * 获取数据加载器列表（子类实现）
     *
     * @return 数据加载器列表
     */
    protected abstract List<DataLoader<REQUEST, CONTEXT>> getDataLoaders();

}