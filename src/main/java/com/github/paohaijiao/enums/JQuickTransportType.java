package com.github.paohaijiao.enums;

public enum JQuickTransportType {

    SOCKET,          // TSocket (阻塞)

    NONBLOCKING,     // TNonblockingSocket (非阻塞)

    HTTP,            // THttpClient

    FRAMED           // TFramedTransport (需配合非阻塞)
}
