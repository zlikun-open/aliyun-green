package com.zlikun.open.helper;

/**
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2018-03-13 17:02
 */
@FunctionalInterface
public interface ResponseHandler<T> {

    void handle(T t);

}
