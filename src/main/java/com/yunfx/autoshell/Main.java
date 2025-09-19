package com.yunfx.autoshell;

import javafx.application.Application;
import javafx.stage.Stage;
import com.yunfx.autoshell.ui.MainController;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController();
        controller.initialize(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
