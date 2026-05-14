package com.github.paohaijiao.config;

import com.github.paohaijiao.enums.JQuickProtocolEnums;
import lombok.Data;

/**
 * 协议配置
 */
@Data
public class JQuickProtocolConfig {

    private String type = "binary";  // binary, compact, json

    private boolean strictRead = true;

    private boolean strictWrite = true;

    private int stringLengthLimit = 0;

    private int containerLengthLimit = 0;

    public static JQuickProtocolConfig binary() {
        JQuickProtocolConfig config = new JQuickProtocolConfig();
        config.setType(JQuickProtocolEnums.binary.getCode());
        return config;
    }

    public static JQuickProtocolConfig compact() {
        JQuickProtocolConfig config = new JQuickProtocolConfig();
        config.setType(JQuickProtocolEnums.compact.getCode());
        return config;
    }

    public static JQuickProtocolConfig json() {
        JQuickProtocolConfig config = new JQuickProtocolConfig();
        config.setType(JQuickProtocolEnums.json.getCode());
        return config;
    }
}
