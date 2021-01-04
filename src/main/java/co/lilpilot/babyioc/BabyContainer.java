package co.lilpilot.babyioc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class BabyContainer {

    /**
     * 获取对象
     * @param clazz  目标类
     * @param <T>    目标泛型
     * @return       目标对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> clazz) {
        List<Constructor<?>> constructorList = new ArrayList<>();
        // 获取无参构造器
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() > 0) {
                continue;
            }
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            constructorList.add(constructor);
        }
        if (constructorList.size() > 1) {
            throw new RuntimeException("duplicated constructor for injection class " + clazz.getCanonicalName());
        }
        if (constructorList.size() == 0) {
            throw new RuntimeException("no accessible constructor for injection class " + clazz.getCanonicalName());
        }
        T target;
        try {
            target = (T) constructorList.get(0).newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("create instance from constructor error", e);
        }
        return target;
    }
}
