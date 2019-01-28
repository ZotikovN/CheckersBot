package game;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoseScreen {
    private static final double screenSize = 450.0;

    public void winnerScreen() {
        Stage stageScreen = new Stage();
        Pane screen = new Pane();
        BackgroundImage boardImg = new BackgroundImage(
                new Image("img/youlose.png", screenSize, screenSize,false,true),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        screen.setBackground(new Background(boardImg));
        Scene scene = new Scene(screen, screenSize, screenSize);
        stageScreen.setTitle("");
        stageScreen.setScene(scene);
        stageScreen.show();
    }
}
