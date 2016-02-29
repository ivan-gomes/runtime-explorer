package com.ivangomes.dev.tools.runtime.ui.tree_item;

import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class QueriableTreeItem<T> extends TreeItem<T> {

    private static ExecutorService EXECUTOR_SERVICE;

    private boolean queried;

    public QueriableTreeItem() {
        this(null);
    }

    public QueriableTreeItem(final T value) {
        super(value);
        this.expandedProperty().addListener((observable, oldValue, newValue) -> {
            final Runnable runnable = () -> {
                try {
                    if (newValue) {
                        this.getChildren().setAll(query());
                    } else {
                        this.getChildren().clear();
                    }
                    queried = newValue;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            if (EXECUTOR_SERVICE != null) {
                EXECUTOR_SERVICE.submit(runnable);
            } else {
                runnable.run();
            }
        });
    }

    public abstract List<TreeItem<T>> query();

    @Override
    public boolean isLeaf() {
        return getValue() == null || queried && super.isLeaf();
    }

    public static ExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }

    public static void setExecutorService(final ExecutorService executorService) {
        EXECUTOR_SERVICE = executorService;
    }
}
