package com.github.paohaijiao.enums;

import lombok.Getter;

@Getter
public enum JQuickServerTypeEnums {

    threadpool("threadpool", "threadpool"),

    threaded("threaded", "threaded"),

    nonblocking("nonblocking", "nonblocking"),

    hsha("hsha", "hsha"),

    selector("selector", "selector");


    private final String name;

    private final String code;

    JQuickServerTypeEnums(String name, String code) {
        this.name = name;
        this.code = code;
    }
}
