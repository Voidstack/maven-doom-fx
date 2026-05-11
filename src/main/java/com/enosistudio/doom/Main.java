package com.enosistudio.doom;

import com.enosistudio.doom.javafx.DoomFXNode;
import com.enosistudio.doom.mochadoom.Engine;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws Exception {
//        DoomFXNode doomNode = Engine.startJavaFX("C:\\ProjectJava\\freedoom1.wad");
        DoomFXNode doomNode = Engine.startJavaFX("app\\doom\\freedoom1.wad");

        StackPane root = new StackPane(doomNode);
        // DoomFXNode doit pouvoir grandir au-delà de son prefSize (320×200)
        doomNode.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Scene scene = new Scene(root, 960, 600);
        stage.setScene(scene);
        stage.setTitle("Doom FX");
        stage.setResizable(true);
        stage.show();

        doomNode.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
