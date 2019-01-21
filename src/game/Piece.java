package game;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

class Piece extends ImageView {
    int row;
    int col;
    private Side side;
    private int lastRow;
    private boolean king = false;

    Piece(Side _side, Image img, double fieldSize, int _row, int _col) {
        super(img);
        this.setFitHeight(fieldSize / 8.0);
        this.setFitWidth(fieldSize / 8.0);
        this.side = _side;
        lastRow = side == Side.HUMAN ? 0 : 7;
        row = _row;
        col = _col;
    }

    boolean hasSide(Side _side) {
        return side == _side;
    }
    boolean isKing() {
        return king;
    }

    public void tryToBecomeKing() {
        if(row == lastRow) {
            this.setImage(new Image(
                    side == Side.HUMAN ? "img/whiteKing.png" : "img/blackKing.png"
            ));
            king = true;
        }
    }
}