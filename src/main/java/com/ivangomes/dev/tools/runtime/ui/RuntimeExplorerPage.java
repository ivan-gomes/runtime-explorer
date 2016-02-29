package com.ivangomes.dev.tools.runtime.ui;

import com.ivangomes.dev.tools.runtime.ui.tree_item.QueriableTreeItem;
import com.ivangomes.dev.tools.util.ClassUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Created by Ivan on 1/20/2016.
 */
public class RuntimeExplorerPage extends VBox implements Initializable {

    @FXML
    private TreeTableView<Pair<AnnotatedElement, Object>> entitiesTreeTableView;
    @FXML
    private TreeTableColumn<Pair<AnnotatedElement, Object>, String> entityValueTreeTableColumn, entityCommentTreeTableColumn;
    @FXML
    private TreeTableColumn<Pair<AnnotatedElement, Object>, Node> entityObjectTreeTableColumn;;

    private static final ObservableValue<String> NULL_STRING_PROPERTY = new SimpleStringProperty("null"), EMPTY_STRING_PROPERTY = new SimpleStringProperty("empty");
    private static ObservableValue<Node> NULL_NODE_VALUE;

    public RuntimeExplorerPage() throws IOException {
        InputStream fxmlInputStream = getClass().getResourceAsStream("/resources/RuntimeExplorerPage.fxml");
        final FXMLLoader loader = new FXMLLoader();
        loader.setController(this);
        loader.setRoot(this);
        loader.load(fxmlInputStream);
        getStylesheets().add(getClass().getResource("/resources/RuntimeExplorerPage.css").toExternalForm());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (NULL_NODE_VALUE == null) {
            // needs to be initialized down here to prevent the race condition with internal graphics initialization
            NULL_NODE_VALUE = new SimpleObjectProperty<>(new Text("null"));
        }
        final Callback<TreeTableColumn.CellDataFeatures<Pair<AnnotatedElement, Object>, Node>, ObservableValue<Node>> objectCallback = param -> {
            if (param.getValue().getValue().getKey() == null) {
                if (param.getValue().getValue().getValue() == null) {
                    return NULL_NODE_VALUE;
                } else if (Map.Entry.class.isAssignableFrom(param.getValue().getValue().getValue().getClass())) {
                    return new SimpleObjectProperty<>(new Text(optionallyThreadedCall(() -> cleanToString(((Map.Entry<?, ?>) param.getValue().getValue().getValue()).getKey()))));
                } else if (param.getValue().getValue().getValue() instanceof Class) {
                    return new SimpleObjectProperty<>(new Text(((Class) param.getValue().getValue().getValue()).getSimpleName()));
                }
                return new SimpleObjectProperty<>(new Text(optionallyThreadedCall(() -> cleanToString(param.getValue().getValue().getValue()))));
            }
            final AnnotatedElement ae = param.getValue().getValue().getKey();
            final Class<?> typeClazz;
            final String nameString;
            switch (ae.getClass().getSimpleName()) {
                case "Method":
                    typeClazz = ((Method) ae).getReturnType();
                    nameString = ((Method) ae).getName();
                    break;
                case "Package":
                    typeClazz = ((Package) ae).getClass();
                    nameString = ((Package) ae).getName();
                    break;
                default:
                    typeClazz = Object.class;
                    nameString = "<Unknown>";
            }
            final Text typeText = new Text(typeClazz.getSimpleName());
            typeText.getStyleClass().addAll("type", "type-" + (ClassUtil.isPrimitiveOrWrapper(typeClazz) ? "primitive" : Integer.toString(typeClazz.getSimpleName().charAt(0) % 16)));
            final Text spaceText = new Text(" ");
            final Text nameText = new Text(nameString);
            final HBox hbox = new HBox(typeText, spaceText, nameText);
            hbox.setAlignment(Pos.CENTER_LEFT);
            return new SimpleObjectProperty<>(hbox);
            //return new SimpleObjectProperty<>(new Text(param.getValue().getValue().getKey().getReturnType().getSimpleName() + " " + param.getValue().getValue().getKey().getName()));
        };
        final Callback<TreeTableColumn.CellDataFeatures<Pair<AnnotatedElement, Object>, String>, ObservableValue<String>> valueCallback = param -> {
            if (param.getValue().getValue().getKey() == null) {
                if (param.getValue().getValue().getValue() != null && Map.Entry.class.isAssignableFrom(param.getValue().getValue().getValue().getClass())) {
                    return new SimpleStringProperty(optionallyThreadedCall(() -> cleanToString(((Map.Entry<?, ?>) param.getValue().getValue().getValue()).getValue())));
                }
                return null;
            } else if (param.getValue().getValue() == null || param.getValue().getValue().getValue() == null) {
                return NULL_STRING_PROPERTY;
            } else if (Iterable.class.isAssignableFrom(param.getValue().getValue().getValue().getClass())) {
                if (!((Iterable) param.getValue().getValue().getValue()).iterator().hasNext()) {
                    return EMPTY_STRING_PROPERTY;
                }
                return null;
            } else if (param.getValue().getValue().getValue().getClass().isArray()) {
                if (Array.getLength(param.getValue().getValue().getValue()) == 0) {
                    return EMPTY_STRING_PROPERTY;
                }
                return null;
            } else if (Map.class.isAssignableFrom(param.getValue().getValue().getValue().getClass())) {
                if (((Map) param.getValue().getValue().getValue()).isEmpty()) {
                    return EMPTY_STRING_PROPERTY;
                }
                return null;
            }
            return new SimpleStringProperty(optionallyThreadedCall(() -> cleanToString(param.getValue().getValue().getValue())));
        };
        final Callback<TreeTableColumn.CellDataFeatures<Pair<AnnotatedElement, Object>, String>, ObservableValue<String>> commentCallback = param -> {
            Object object = param.getValue().getValue().getValue();
            if (object == null) {
                return null;
            }
            //TODO Implement comments
            return new SimpleStringProperty("");
        };

        entityObjectTreeTableColumn.setCellValueFactory(objectCallback);
        entityObjectTreeTableColumn.setComparator((o1, o2) ->
                o1.lookupAll("Text").stream().filter(o -> o instanceof Text).map(o -> ((Text) o).getText()).reduce((s, s2) -> s + s2).orElse(o1.toString()).compareTo(
                o2.lookupAll("Text").stream().filter(o -> o instanceof Text).map(o -> ((Text) o).getText()).reduce((s, s2) -> s + s2).orElse(o2.toString())
        ));
        entityValueTreeTableColumn.setCellValueFactory(valueCallback);
        entityCommentTreeTableColumn.setCellValueFactory(commentCallback);
    }

    private static <T> T optionallyThreadedCall(Callable<T> callable) {
        if (QueriableTreeItem.getExecutorService() != null) {
            try {
                return QueriableTreeItem.getExecutorService().submit(callable).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            try {
                return callable.call();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public TreeTableView<Pair<AnnotatedElement, Object>> getEntitiesTreeTableView() {
        return entitiesTreeTableView;
    }

    public static final Map<Class<?>, Function<Object, String>> OVERRIDDEN_TO_STRINGS = new HashMap<>(5);

    private static String cleanToString(Object o) {
        return OVERRIDDEN_TO_STRINGS.getOrDefault(o.getClass(), Object::toString).apply(o);
    }
}
