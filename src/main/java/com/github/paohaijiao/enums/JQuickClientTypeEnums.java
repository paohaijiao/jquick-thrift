package com.github.paohaijiao.enums;

import lombok.Getter;

@Getter
public enum JQuickClientTypeEnums {

    pooled("pooled", "pooled"),

    single("single", "single"),

    multiplexed("multiplexed", "multiplexed");


    private final String name;

    private final String code;

    JQuickClientTypeEnums(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
