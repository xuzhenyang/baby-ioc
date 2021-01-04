package co.lilpilot.babyioc;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Consumer;

public class BabyContainer {

    private final Map<Class<?>, Object> singletons = Collections.synchronizedMap(new HashMap<>());

    private final Set<Class<?>> processingClasses = Collections.synchronizedSet(new HashSet<>());

    private final Map<Class<?>, Map<Annotation, Object>> qualifiers = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Map<Annotation, Class<?>>> qualifiedClass = Collections.synchronizedMap(new HashMap<>());

    /**
     * 注册限定类
     * @param parentClass  父类
     * @param clazz        目标类
     * @param <T>
     * @return
     */
    public <T> BabyContainer registerQualifiedClass(Class<?> parentClass, Class<T> clazz) {
        for (Annotation annotation : clazz.getDeclaredAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                registerQualifiedClass(parentClass, clazz, annotation);
            }
        }
        return this;
    }

    private <T> void registerQualifiedClass(Class<?> parentClass, Class<T> clazz, Annotation annotation) {
        if (!annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
            throw new RuntimeException("没有声明Qualifier");
        }
        Map<Annotation, Class<?>> annotationObjectMap = qualifiedClass.computeIfAbsent(parentClass, k -> Collections.synchronizedMap(new HashMap<>()));
        if (annotationObjectMap.put(annotation, clazz) != null) {
            throw new RuntimeException("重复注册");
        }
    }

    /**
     * 获取对象
     * @param clazz  目标类
     * @param <T>    目标泛型
     * @return       目标对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> clazz) {
        return createNew(clazz);
    }
    private <T> T createNew(Class<T> clazz) {
        return this.createNew(clazz, null);
    }

    private <T> T createNew(Class<T> clazz, Consumer<T> comsumer) {
        Object object = singletons.get(clazz);
        if (object != null) {
            return (T) object;
        }
        List<Constructor<T>> constructorList = new ArrayList<>();
        // 获取构造器
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            // 无参构造器不过滤
            if (!constructor.isAnnotationPresent(Inject.class) && constructor.getParameterCount() > 0) {
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
        processingClasses.add(clazz);
        T target = createFromConstructor(constructorList.get(0));
        processingClasses.remove(clazz);
        boolean isSingleton = clazz.isAnnotationPresent(Singleton.class);
        if (isSingleton) {
            singletons.put(clazz, target);
        }
        if (comsumer != null) {
            comsumer.accept(target);
        }
        //注入成员
        injectMembers(target);
        return target;
    }

    private <T> T createFromQualified(Class<?> clazz, Class<T> declaringClass, Annotation[] annotations) {
        Map<Annotation, Object> annotationObjectMap = qualifiers.get(clazz);
        if (annotationObjectMap != null) {
            Set<Object> set = new HashSet<>();
            for (Annotation annotation : annotations) {
                Object obj = annotationObjectMap.get(annotation);
                if (obj != null) {
                    set.add(obj);
                }
            }
            if (set.size() > 1) {
                throw new RuntimeException("限定实例重复");
            }
            if (!set.isEmpty()) {
                return (T) set.iterator().next();
            }
        }
        Map<Annotation, Class<?>> annotationClassMap = qualifiedClass.get(clazz);
        if (annotationClassMap != null) {
            Set<Class<?>> classSet = new HashSet<>();
            Annotation targetAnno = null;
            for (Annotation annotation : annotations) {
                Class<?> aClass = annotationClassMap.get(annotation);
                if (aClass != null) {
                    classSet.add(aClass);
                    targetAnno = annotation;
                }
            }
            if (classSet.size() > 1) {
                throw new RuntimeException("限定类注册重复");
            }
            if (!classSet.isEmpty()) {
                Class<?> targetClass = classSet.iterator().next();
                final Annotation finalTargetAnno = targetAnno;
                //callback塞入限定容器 避免循环依赖
                Object instance = createNew(targetClass, o -> registerQualifier(targetClass, finalTargetAnno, o));
                return (T) instance;
            }
        }
        return null;
    }

    private <T> void registerQualifier(Class<T> clazz, Annotation annotation, Object Object) {
        if (!annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
            throw new RuntimeException("没有声明Qualifier");
        }
        Map<Annotation, Object> annotationObjectMap =
                qualifiers.computeIfAbsent(clazz, k -> Collections.synchronizedMap(new HashMap<>()));
        if (annotationObjectMap.put(annotation, Object) != null) {
            throw new RuntimeException("重复Qualifier");
        }

    }

    private <T> T createFromConstructor(Constructor<T> constructor) {
        int paramIndex = 0;
        Object[] params = new Object[constructor.getParameterCount()];
        for (Parameter parameter : constructor.getParameters()) {
            if (processingClasses.contains(parameter.getType())) {
                throw new RuntimeException("循环依赖");
            }
            Object param = createFromParameter(parameter);
            params[paramIndex++] = param;
        }

        try {
            return constructor.newInstance(params);
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
        Object instance = createFromQualified(type, field.getDeclaringClass(), field.getAnnotations());
        if (instance != null) {
            return (T) instance;
        }
        return (T) createNew(type);
    }

    private <T> T createFromParameter(Parameter parameter) {
        Class<?> type = parameter.getType();
        Object instance = createFromQualified(type, parameter.getDeclaringExecutable().getClass(), parameter.getAnnotations());
        if (instance != null) {
            return (T) instance;
        }
        return (T) createNew(type);
    }
}
