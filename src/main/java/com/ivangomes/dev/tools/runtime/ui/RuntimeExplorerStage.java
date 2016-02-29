package com.ivangomes.dev.tools.runtime.ui;

import com.ivangomes.dev.tools.runtime.RuntimeExplorer;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class RuntimeExplorerStage extends Stage {

    @Getter
    private final RuntimeExplorerPage page;

    @SneakyThrows(IOException.class)
    public RuntimeExplorerStage() {
        setTitle("Runtime Explorer");
        if (Thread.currentThread().getContextClassLoader() instanceof URLClassLoader) {
            setTitle(getTitle() + " - " + Arrays.asList(((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs())
                    .stream().map(URL::getFile).reduce((s, s2) -> s + (s.isEmpty() ? "" : ", ") + s2).orElse("<>"));
        }
        setScene(new Scene(page = new RuntimeExplorerPage()));
        setOnCloseRequest(event -> RuntimeExplorer.getInstance().getStages().remove(this));
    }
}
