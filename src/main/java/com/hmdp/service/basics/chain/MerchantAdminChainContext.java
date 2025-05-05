package com.hmdp.service.basics.chain;

import cn.hutool.core.collection.CollectionUtil;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 15:42
 * @Description: 商家后管责任链上下文容器
 */
@Component
public final class MerchantAdminChainContext<T> implements ApplicationContextAware, CommandLineRunner {

    /**
     * 应用上下文，通过 SpringIOC 获取 Bean 实例
     */
    private ApplicationContext applicationContext;

    private final Map<String, List<MerchantAdminAbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * 责任链组件执行
     * @param mark 责任链组件标识
     * @param requestParam 请求参数
     */
    public void handler(String mark, T requestParam) {
        List<MerchantAdminAbstractChainHandler> handlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtil.isEmpty(handlers)) {
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        handlers.forEach(each -> each.handler(requestParam));
    }

    @Override
    public void run(String... args) throws Exception {
        // 从 SpringIoC 容器中获取指定接口 SpringBean集合
        Map<String, MerchantAdminAbstractChainHandler> chainFilterMap = applicationContext.getBeansOfType(MerchantAdminAbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            // 判断 Mark 是否已经存在抽象责任链容器中，如果以进存在直接向结合新增；如果不存在，创建 Mark 和对应的结合
            List<MerchantAdminAbstractChainHandler> chainHandlers = abstractChainHandlerContainer.getOrDefault(bean.mark(), new ArrayList<>());
            chainHandlers.add(bean);
            abstractChainHandlerContainer.put(bean.mark(), chainHandlers);
        });
        abstractChainHandlerContainer.forEach((mark, unsortedChainHandlers) ->
                // 对每个 mark 对应的责任链实现类集合进行排序，优先级小的在前
                unsortedChainHandlers.sort(Comparator.comparing(Ordered::getOrder))
        );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
