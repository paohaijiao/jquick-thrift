package com.github.paohaijiao.enums;

import lombok.Getter;

@Getter
public enum JQuickProtocolEnums {

    binary("binary", "binary"),

    compact("compact", "compact"),

    json("json", "json");

    private final String name;

    private final String code;

    JQuickProtocolEnums(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
