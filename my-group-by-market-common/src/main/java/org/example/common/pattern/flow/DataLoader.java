package org.example.common.pattern.flow;

/**
 * 数据加载器接口
 * 用于节点执行前的数据预加载
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 流程上下文类型
 */
public interface DataLoader<REQUEST, CONTEXT> {

    /**
     * 加载数据到上下文
     *
     * @param request 请求参数
     * @param context 流程上下文
     */
    void loadData(REQUEST request, CONTEXT context);

    /**
     * 获取加载器名称
     *
     * @return 加载器名称
     */
    default String getLoaderName() {
        return this.getClass().getSimpleName();
    }
}