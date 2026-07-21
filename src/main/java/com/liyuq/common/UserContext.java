package com.liyuq.common;
import org.springframework.stereotype.Component;


@Component
public class UserContext {

    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    public static void SetUserContext(Long userId) {
     threadLocal.set(userId);
    }

    public static void removeUserContextId() {
        threadLocal.remove();
    }
    public static Long getUserContextId() {
        return threadLocal.get();
    }
}
