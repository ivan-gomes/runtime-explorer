package com.ivangomes.dev.tools.runtime.ui.tree_item;

import javafx.scene.control.TreeItem;
import javafx.util.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PackageTreeItem extends QueriableTreeItem<Pair<AnnotatedElement, Object>> {

    private final Package pakkage;
    private static final String REGEX_CHILD_SUFFIX = "[^.]+$";


    public PackageTreeItem(final Package pakkage) {
        super(new Pair<>(pakkage, ""));
        this.pakkage = pakkage;
    }

    @Override
    public List<TreeItem<Pair<AnnotatedElement, Object>>> query() {
        final List<TreeItem<Pair<AnnotatedElement, Object>>> results = new ArrayList<>();
        final Predicate<String> childPredicate = Pattern.compile("^" + (pakkage != null ? "\\." + pakkage.getName() : "") + REGEX_CHILD_SUFFIX).asPredicate();
        Arrays.asList(Package.getPackages()).stream().filter(p -> childPredicate.test(p.getName())).map(PackageTreeItem::new).forEach(results::add);

        if (pakkage != null) {
            final Reflections reflections = new Reflections(pakkage.getName(), new SubTypesScanner(false));
            reflections.getStore().getSubTypesOf(Object.class.getName()).stream().map(s -> {
                try {
                    return Class.forName(s);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }).map(c -> ((QueriableTreeItem) new ReflectiveTreeItem.StaticReflectiveTreeItem(c))).filter(qti -> !qti.query().isEmpty()).forEach(results::add);
        }
        return results;
    }
}
