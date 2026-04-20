package com.github.paohaijiao.constants;

import com.github.paohaijiao.spi.constants.PriorityConstants;

public class JQuickThriftPriorityConstants {

    /**
     * 核心服务（最高优先级）
     */
    public static final int CORE_SERVICE = PriorityConstants.SYSTEM_HIGHEST;
    /**
     * 基础设施服务
     */
    public static final int INFRASTRUCTURE = PriorityConstants.SYSTEM_HIGH;

    /**
     * 业务服务
     */
    public static final int BUSINESS_SERVICE = PriorityConstants.BUSINESS_MEDIUM;

    /**
     * 扩展服务
     */
    public static final int EXTENSION_SERVICE = PriorityConstants.USER_HIGH;

    /**
     * 可选服务
     */
    public static final int OPTIONAL_SERVICE = PriorityConstants.LOWEST;

    private JQuickThriftPriorityConstants() {}
}
