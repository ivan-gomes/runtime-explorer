package com.ivangomes.dev.tools.runtime;

import com.ivangomes.dev.tools.runtime.ui.*;
import com.ivangomes.dev.tools.runtime.ui.tree_item.PackageTreeItem;
import com.ivangomes.dev.tools.runtime.ui.tree_item.QueriableTreeItem;
import com.ivangomes.dev.tools.runtime.ui.tree_item.ReflectiveTreeItem;
import com.sun.javafx.css.StyleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Getter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Getter
public class RuntimeExplorer extends Application implements Runnable {
    private static RuntimeExplorer INSTANCE;

    private final ObservableList<RuntimeExplorerStage> stages = FXCollections.observableArrayList();

    {
        stages.addListener((ListChangeListener<RuntimeExplorerStage>) c -> {
            if (c.getList().isEmpty()) {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    // TODO Make thread count configurable with arguments
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    static {
        ReflectiveTreeItem.BLACKLISTED_METHODS.addAll(Arrays.asList(
                new Pair<>(Object.class, "getClass"),
                new Pair<>(Enum.class, "getDeclaringClass")
        ));

        RuntimeExplorerPage.OVERRIDDEN_TO_STRINGS.put(Point.class, o -> {
            Point p = (Point) o;
            return "Point(" + p.x + ", " + p.y + ")";
        });
        RuntimeExplorerPage.OVERRIDDEN_TO_STRINGS.put(Color.class, o -> {
            Color c = (Color) o;
            return "Rgba(" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ", " + c.getAlpha() + ")";
        });
        RuntimeExplorerPage.OVERRIDDEN_TO_STRINGS.put(Dimension.class, o -> {
            Dimension d = (Dimension) o;
            return "Dimension(" + d.width + " x " + d.height + ")";
        });
        RuntimeExplorerPage.OVERRIDDEN_TO_STRINGS.put(Rectangle.class, o -> {
            Rectangle r = (Rectangle) o;
            return "Rectangle(" + r.x + ", " + r.y + ", " + r.width + ", " + r.height + ")";
        });
        RuntimeExplorerPage.OVERRIDDEN_TO_STRINGS.put(Rectangle2D.Double.class, o -> {
            Rectangle2D.Double rd = (Rectangle2D.Double) o;
            return "Rectangle2D(" + rd.x + ", " + rd.y + ", " + rd.width + ", " + rd.height + ")";
        });
    }

    public static RuntimeExplorer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RuntimeExplorer();
        }
        return INSTANCE;
    }

    @Override
    public void run() {
        QueriableTreeItem.setExecutorService(executorService);
        final RuntimeExplorerStage stage = new RuntimeExplorerStage();
        //stage.getPage().getEntitiesTreeTableView().getRoot().getChildren().add(new ReflectiveTreeItem.StaticReflectiveTreeItem(System.class));
        stage.getPage().getEntitiesTreeTableView().setRoot(new QueriableTreeItem<Pair<AnnotatedElement, Object>>() {
            @Override
            public List<TreeItem<Pair<AnnotatedElement, Object>>> query() {
                return Arrays.asList(Package.getPackages()).stream().sorted((o1, o2) -> o1.getName().compareTo(o2.getName())).map(PackageTreeItem::new).collect(Collectors.toList());
            }

            @Override
            public boolean isLeaf() {
                return false;
            }
        });
        stages.add(stage);
        stage.show();
    }

    private static TreeItem<Pair<Method, Object>> buildPseudoRootTreeItem(final String name, final Callable<Collection<?>> query) {
        return new QueriableTreeItem<Pair<Method, Object>>(new Pair<>(null, name)) {
            @Override
            public List<TreeItem<Pair<Method, Object>>> query() {
                try {
                    return query.call().stream().map(i -> new ReflectiveTreeItem(null, i)).collect(Collectors.toList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Collections.emptyList();
            }

            @Override
            public boolean isLeaf() {
                return false;
            }
        };
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        RuntimeExplorer.getInstance().run();

        final URL stylesheetUrl = getClass().getResource("/resources/spotifx.css");
        if (stylesheetUrl != null) {
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
            StyleManager.getInstance().addUserAgentStylesheet(stylesheetUrl.toExternalForm());
        }
    }

    public static void main(String... args) {
        applyConfiguration(args);
        Application.launch(args);
    }

    @SuppressWarnings("unused")
    private static void applyConfiguration(final String... args) {
        // TODO Implement POSIX/GNU options
    }
}
