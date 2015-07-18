package ru.samlib.client.util;


import ru.samlib.client.R;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by 0shad on 13.07.2015.
 */
public class ReflectionUtils {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Value {
        String name();
    }

    public static <S, T> T castObject(S source, Class<T> targetClass) throws IllegalAccessException, InstantiationException {
        T newInstance = targetClass.newInstance();
        for (Field field : source.getClass().getDeclaredFields()) {
            for (Field fieldTarget : targetClass.getDeclaredFields()) {
                if (isFieldsEqual(field, fieldTarget)) {
                    setField(getField(field, source), fieldTarget, newInstance);
                }
            }
        }
        return newInstance;
    }

    private static boolean isFieldsEqual(Field one, Field two) {
        return one.getName().equals(two.getName()) &&
                one.getType().equals(two);
    }

    public static Map<String, Object> getClassValues(Object obj) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (Field field : getAllFieldsValues(obj.getClass())) {
            Value annotation;
            // check if field has annotation
            if ((annotation = field.getAnnotation(Value.class)) != null) {
                String name = annotation.name();
                if (name == null) {
                    name = field.getName();
                }
                values.put(name, getField(field, obj));
            }
        }
        return values;
    }


    public static Map<String, Object> getClassFields(Object obj) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (Field field : getAllFieldsValues(obj.getClass())) {
            values.put(field.getName(), getField(field, obj));
        }
        return values;
    }

    public static Object getField(Field field, Object source) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object result = null;
        try {
            result = field.get(source);
        } catch (IllegalAccessException e) {
            // ignore
        }
        field.setAccessible(accessible);
        return result;
    }

    public static void setField(Object value, Field field, Object target) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object result = null;
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            // ignore
        }
        field.setAccessible(accessible);
    }

    /**
     * Return a list of all fields (whatever access status, and on whatever
     * superclass they were defined) that can be found on this class.
     * This is like a union of {@link Class#getDeclaredFields()} which
     * ignores and super-classes, and {@link Class#getFields()} which ignored
     * non-public fields
     *
     * @param clazz The class to introspect
     * @return The complete list of fields
     */
    public static Field[] getAllFieldsValues(Class<?> clazz) {
        List<Class<?>> classes = getAllSuperclasses(clazz);
        classes.add(clazz);
        return getAllFields(classes);
    }


    /**
     * Return a list of all fields (whatever access status, and on whatever
     * superclass they were defined) that can be found on this class.
     * This is like a union of {@link Class#getDeclaredFields()} which
     * ignores and super-classes, and {@link Class#getFields()} which ignored
     * non-public fields
     *
     * @param clazz The class to introspect
     * @return The complete list of fields
     */
    public static Field[] getAllFields(Class<?> clazz) {
        List<Class<?>> classes = getAllSuperclasses(clazz);
        classes.add(clazz);
        return getAllFields(classes);
    }

    /**
     * As {@link #getAllFields(Class)} but acts on a list of {@link Class}s and
     * uses only {@link Class#getDeclaredFields()}.
     *
     * @param classes The list of classes to reflect on
     * @return The complete list of fields
     */
    private static Field[] getAllFields(List<Class<?>> classes) {
        Set<Field> fields = new HashSet<Field>();
        for (Class<?> clazz : classes) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }

        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Return a List of super-classes for the given class.
     *
     * @param clazz the class to look up
     * @return the List of super-classes in order going up from this one
     */
    public static List<Class<?>> getAllSuperclasses(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null) {
            classes.add(superclass);
            superclass = superclass.getSuperclass();
        }

        return classes;
    }

    public static <T extends Object> T newInstance(Class<T> cl, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<T> constructor = cl.getDeclaredConstructor(new Class[0]);
        boolean accessible = constructor.isAccessible();
        constructor.setAccessible(true);
        T t = constructor.newInstance(args);
        constructor.setAccessible(accessible);
        return t;
    }


    public static <T extends Object> void callMethod(T receiver, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        callMethodCast(receiver, null, methodName, args);
    }

    public static <T extends Object, Result> Result callMethodCast(T receiver, Class<Result> resultClass, String methodName, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (receiver == null || methodName == null) {
            return null;
        }
        Class<?> cls = receiver.getClass();
        Method toInvoke = null;
        do {
            Method[] methods = cls.getDeclaredMethods();
            methodLoop:
            for (Method method : methods) {
                if (!methodName.equals(method.getName())) {
                    continue;
                }
                Class<?>[] paramTypes = method.getParameterTypes();
                if (args == null && paramTypes == null) {
                    toInvoke = method;
                    break;
                } else if (args == null || paramTypes == null
                        || paramTypes.length != args.length) {
                    continue;
                }

                for (int i = 0; i < args.length; ++i) {
                    if (!paramTypes[i].isAssignableFrom(args[i].getClass())) {
                        continue methodLoop;
                    }
                }
                toInvoke = method;
            }
        } while (toInvoke == null && (cls = cls.getSuperclass()) != null);
        Result t;
        if (toInvoke != null) {
            boolean accessible = toInvoke.isAccessible();
            toInvoke.setAccessible(true);
            if (resultClass != null) {
                t = resultClass.cast(toInvoke.invoke(receiver, args));
            } else {
                toInvoke.invoke(receiver, args);
                t = null;
            }
            toInvoke.setAccessible(accessible);
            return t;
        } else {
            throw new NoSuchMethodException("Method " + methodName + " not found");
        }
    }
}
