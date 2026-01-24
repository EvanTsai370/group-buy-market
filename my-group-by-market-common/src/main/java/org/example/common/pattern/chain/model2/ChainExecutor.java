package org.example.common.pattern.chain.model2;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 责任链执行器（Model2 - 多例链）
 *
 * <p>设计说明：
 * <ul>
 *   <li>职责：管理责任链的结构，负责按顺序执行所有处理器</li>
 *   <li>特点：解耦业务处理器和链路管理，支持动态组装多条链</li>
 *   <li>优势：同一套处理器可以组装成不同的责任链</li>
 *   <li>改进：通过 {@link IChainResponse#shouldContinue()} 显式判断是否继续（替代 null 判断）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 创建处理器
 * IChainHandler<Request, Context, Response> handler1 = new ValidationHandler();
 * IChainHandler<Request, Context, Response> handler2 = new AuthHandler();
 * IChainHandler<Request, Context, Response> handler3 = new BusinessHandler();
 *
 * // 组装责任链1（完整流程）
 * ChainExecutor<Request, Context, Response> chain1 =
 *     new ChainExecutor<>("完整流程", handler1, handler2, handler3);
 *
 * // 组装责任链2（快速通道，跳过权限校验）
 * ChainExecutor<Request, Context, Response> chain2 =
 *     new ChainExecutor<>("快速通道", handler1, handler3);
 *
 * // 执行
 * Response result = chain1.execute(request, context);
 * }</pre>
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 动态上下文类型
 * @param <RESPONSE> 响应结果类型（必须实现 {@link IChainResponse} 接口）
 *
 */
@Slf4j
public class ChainExecutor<REQUEST, CONTEXT, RESPONSE extends IChainResponse> {

    /** 责任链名称（用于日志和调试） */
    private final String chainName;

    /** 处理器列表 */
    private final List<IChainHandler<REQUEST, CONTEXT, RESPONSE>> handlers;

    /**
     * 构造函数
     *
     * @param chainName 责任链名称
     * @param handlers 处理器列表（按顺序执行）
     */
    @SafeVarargs
    public ChainExecutor(String chainName, IChainHandler<REQUEST, CONTEXT, RESPONSE>... handlers) {
        this.chainName = chainName;
        this.handlers = new ArrayList<>(Arrays.asList(handlers));
    }

    /**
     * 添加处理器到链尾
     *
     * @param handler 处理器
     * @return this（支持链式调用）
     */
    public ChainExecutor<REQUEST, CONTEXT, RESPONSE> addHandler(
            IChainHandler<REQUEST, CONTEXT, RESPONSE> handler) {
        this.handlers.add(handler);
        return this;
    }

    /**
     * 添加处理器到链头
     *
     * @param handler 处理器
     * @return this（支持链式调用）
     */
    public ChainExecutor<REQUEST, CONTEXT, RESPONSE> addFirst(
            IChainHandler<REQUEST, CONTEXT, RESPONSE> handler) {
        this.handlers.addFirst(handler);
        return this;
    }

    /**
     * 执行责任链
     *
     * <p>执行逻辑：
     * <ol>
     *   <li>按顺序遍历所有处理器</li>
     *   <li>调用每个处理器的 handle() 方法</li>
     *   <li>如果返回的 response.shouldContinue() == false，则中断链路并返回结果</li>
     *   <li>如果返回的 response.shouldContinue() == true，则继续下一个处理器</li>
     *   <li>如果所有处理器都返回 shouldContinue() == true，则返回最后一个响应</li>
     * </ol>
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return 执行结果
     * @throws Exception 业务异常
     */
    public RESPONSE execute(REQUEST request, CONTEXT context) throws Exception {
        if (handlers.isEmpty()) {
            return null;
        }

        RESPONSE lastResponse = null;
        for (IChainHandler<REQUEST, CONTEXT, RESPONSE> handler : handlers) {
            RESPONSE response = handler.handle(request, context);

            // 如果不应该继续，则中断链路
            if (!response.shouldContinue()) {
                log.info("【{}】责任链中断, handler: {}, reason: {}",
                        chainName, handler.getClass().getSimpleName(), response.getReason());
                return response;
            }

            lastResponse = response;
            // 如果 shouldContinue() == true，继续下一个处理器
        }

        // 所有处理器都放行，返回最后一个响应
        log.debug("【{}】责任链执行完成，所有处理器放行", chainName);
        return lastResponse;
    }

    /**
     * 获取责任链名称
     *
     * @return 责任链名称
     */
    public String getChainName() {
        return chainName;
    }

    /**
     * 获取处理器数量
     *
     * @return 处理器数量
     */
    public int size() {
        return handlers.size();
    }

    /**
     * 打印责任链信息（用于调试）
     */
    public void printChainInfo() {
        System.out.println("========================================");
        System.out.println("责任链: " + chainName);
        System.out.println("处理器数量: " + handlers.size());
        System.out.println("处理器列表:");
        for (int i = 0; i < handlers.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + handlers.get(i).getClass().getSimpleName());
        }
        System.out.println("========================================");
    }
}
