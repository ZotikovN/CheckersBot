package game;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

class Selection extends ImageView {
    Piece target = null;
    Selection() {
        super(new Image("img/selection.png"));
    }
    boolean isSet() {
        return target != null;
    }
}
