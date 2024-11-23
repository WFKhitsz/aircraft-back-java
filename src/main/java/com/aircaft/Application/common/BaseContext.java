package com.aircaft.Application.common;

public class BaseContext {

    public static ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    public static Integer getCurrentId() {
        return threadLocal.get();
    }

    public static void setCurrentId(Integer id) {
        threadLocal.set(id);
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
