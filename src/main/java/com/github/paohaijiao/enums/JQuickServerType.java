package com.github.paohaijiao.enums;

public enum JQuickServerType {

    SIMPLE,          // TSimpleServer (单线程)

    THREAD_POOL,     // TThreadPoolServer (线程池)

    NONBLOCKING,     // TNonblockingServer (NIO)

    HS_HA            // THsHaServer (半同步半异步)
}
