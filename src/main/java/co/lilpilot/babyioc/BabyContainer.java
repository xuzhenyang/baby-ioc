package co.lilpilot.babyioc;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class BabyContainer {

    private final Map<Class<?>, Object> singletons = Collections.synchronizedMap(new HashMap<>());

    /**
     * 获取对象
     * @param clazz  目标类
     * @param <T>    目标泛型
     * @return       目标对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> clazz) {
        Object object = singletons.get(clazz);
        if (object != null) {
            return (T) object;
        }
        return createNew(clazz);
    }

    private <T> T createNew(Class<T> clazz) {
        List<Constructor<T>> constructorList = new ArrayList<>();
        // 获取无参构造器
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() > 0) {
                continue;
            }
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            constructorList.add((Constructor<T>) constructor);
        }
        if (constructorList.size() > 1) {
            throw new RuntimeException("注入的类有重复构造器 " + clazz.getCanonicalName());
        }
        if (constructorList.size() == 0) {
            throw new RuntimeException("注入的类没有可获取的构造器 " + clazz.getCanonicalName());
        }
        T target = createFromConstructor(constructorList.get(0));
        boolean isSingleton = clazz.isAnnotationPresent(Singleton.class);
        if (isSingleton) {
            singletons.put(clazz, target);
        }
        //注入成员
        injectMembers(target);
        return target;
    }

    private <T> T createFromConstructor(Constructor<T> constructor) {
        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("构造器实例化失败", e);
        }
    }

    private <T> void injectMembers(T t) {
        Class<?> tClass = t.getClass();
        List<Field> needToCreateFieldList = new ArrayList<>();
        for (Field field : tClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                needToCreateFieldList.add(field);
            }
        }
        for (Field field : needToCreateFieldList) {
            Object value = createFromField(field);
            try {
                field.set(t, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("字段注入设值失败", e);
            }
        }
    }

    private <T> T createFromField(Field field) {
        Class<?> type = field.getType();
        return (T) createNew(type);
    }
}
