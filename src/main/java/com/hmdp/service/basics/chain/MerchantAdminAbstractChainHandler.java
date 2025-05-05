package com.hmdp.service.basics.chain;

import org.springframework.core.Ordered;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 15:41
 * @Description: MerchantAdminAbstractChainHandler
 */
public interface MerchantAdminAbstractChainHandler<T> extends Ordered {

    /**
     * 执行责任链逻辑
     *
     * @param requestParam 责任链执行入参
     */
    void handler(T requestParam);

    /**
     * @return 责任链组件标识
     */
    String mark();
}
