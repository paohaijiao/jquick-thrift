package com.github.paohaijiao.ano;

import java.lang.annotation.*;

/**
 * Thrift服务注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JQuickThriftService {

    String name() default "";

    int version() default 1;

}
