package com.ivangomes.dev.tools.runtime.ui.tree_item;

import com.ivangomes.dev.tools.util.ClassUtil;
import javafx.scene.control.TreeItem;
import javafx.util.Pair;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReflectiveTreeItem extends QueriableTreeItem<Pair<Method, Object>> {
    private static final Pattern METHOD_REGEX = Pattern.compile("^(get|is)\\w+$");
    public static final List<Pair<Class<?>, String>> BLACKLISTED_METHODS = new ArrayList<>(50);

    public ReflectiveTreeItem(final Method method, final Object value) {
        super(new Pair<>(method, value));
    }

    protected Object getQueryableObject() {
        return getValue().getValue();
    }

    @Override
    public List<TreeItem<Pair<Method, Object>>> query() {
        final Object queryableObject = getQueryableObject();
        if (queryableObject == null) {
            return null;
        }
        return getMethodsToReflect(queryableObject instanceof Class ? (Class) queryableObject : queryableObject.getClass()).stream()
                .map(m -> {
                    Object value;
                    try {
                        m.setAccessible(true);
                        value = m.invoke(queryableObject);
                    } catch (ReflectiveOperationException e) {
                        //System.err.println("Faulty method: " + m.getName());
                        //e.printStackTrace();
                        value = e.getCause() != null ? e.getCause() : e;
                    }
                    final TreeItem<Pair<Method, Object>> treeItem;
                    if (value == null) {
                        treeItem = new TreeItem<>(new Pair<>(m, null));
                    }
                    else if (!isTypeExpandable(m.getReturnType()) && !isTypeExpandable(value.getClass())) {
                        treeItem = new TreeItem<>(new Pair<>(m, value));
                    }
                    else if (Iterable.class.isAssignableFrom(value.getClass())) {
                        treeItem = new TreeItem<>(new Pair<>(m, value));
                        ((Iterable<?>) value).forEach(o -> treeItem.getChildren().add(o == null || isTypeExpandable(o.getClass()) ? new ReflectiveTreeItem(null, o) : new TreeItem<>(new Pair<>(null, o))));
                    }
                    else if (value.getClass().isArray()) {
                        treeItem = new TreeItem<>(new Pair<>(m, value));
                        for (int index = 0, length = Array.getLength(value); index < length; ++index) {
                            Object o = Array.get(value, index);
                            if (o != null) {
                                treeItem.getChildren().add(isTypeExpandable(o.getClass()) ? new ReflectiveTreeItem(null, o) : new TreeItem<>(new Pair<>(null, o)));
                            }
                        }
                    }
                    else if (Map.class.isAssignableFrom(value.getClass())) {
                        treeItem = new TreeItem<>(new Pair<>(m, value));
                        ((Map<?, ?>) value).entrySet().forEach(o -> treeItem.getChildren().add(new ReflectiveTreeItem(null, o)));
                    }
                    else {
                        treeItem = new ReflectiveTreeItem(m, value);
                    }
                    return treeItem;
                }).filter(ti -> ti != null).collect(Collectors.toList());
    }

    @Override
    public boolean isLeaf() {
        return getValue() == null || getValue().getValue() == null || super.isLeaf();
    }

    protected boolean isTypeExpandable(Class<?> c) {
        if (c.isArray() || Iterable.class.isAssignableFrom(c)) {
            return true;
        }
        if (ClassUtil.isPrimitiveOrWrapper(c)) {
            return false;
        }
        if (String.class.equals(c)) {
            return false;
        }
        if (getMethodsToReflect(c).size() == 0) {
            return false;
        }
        return true;
    }

    protected List<Method> getMethodsToReflect(Class<?> c) {
        return Arrays.asList(c.getMethods()).stream().filter(m -> m.getParameterCount() == 0
                && !Modifier.isStatic(m.getModifiers())
                && !m.getReturnType().equals(Void.TYPE)
                && !m.isAnnotationPresent(Deprecated.class)
                && METHOD_REGEX.matcher(m.getName()).matches()
                && !BLACKLISTED_METHODS.stream().anyMatch(pair -> pair.getKey().isAssignableFrom(c) && pair.getValue().equals(m.getName())))
                .collect(Collectors.toList());
    }

    public static class StaticReflectiveTreeItem extends ReflectiveTreeItem {

        public StaticReflectiveTreeItem(Class<?> clazz) {
            super(null, clazz);
        }

        @Override
        protected boolean isTypeExpandable(Class<?> c) {
            if (c.isArray() || Iterable.class.isAssignableFrom(c)) {
                return true;
            }
            if (ClassUtil.isPrimitiveOrWrapper(c)) {
                return false;
            }
            if (String.class.equals(c)) {
                return false;
            }
            if (super.getMethodsToReflect(c).size() == 0) {
                return false;
            }
            return true;
        }

        @Override
        protected List<Method> getMethodsToReflect(Class<?> c) {
            return Arrays.asList(c.getMethods()).stream().filter(m -> m.getParameterCount() == 0
                    && Modifier.isStatic(m.getModifiers())
                    && !m.getReturnType().equals(Void.TYPE)
                    && !m.isAnnotationPresent(Deprecated.class)
                    && (m.getName().equals("values") || METHOD_REGEX.matcher(m.getName()).matches())
                    && !BLACKLISTED_METHODS.stream().anyMatch(pair -> pair.getKey().isAssignableFrom(c) && pair.getValue().equals(m.getName())))
                    .collect(Collectors.toList());
        }
    }
}
