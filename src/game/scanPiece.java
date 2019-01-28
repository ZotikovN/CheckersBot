package game;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ScanPiece{
    int row;
    int col;
    private Side side;
    private int lastRow;
    private boolean king = false;

    ScanPiece(Side _side, int _row, int _col) {
        this.side = _side;
        lastRow = side == Side.HUMAN ? 0 : 7;
        row = _row;
        col = _col;
    }

    int getRow() {
        return row;
    }

    int getCol() {
        return col;
    }



    boolean hasSide(Side _side) {
        return side == _side;
    }
    boolean isKing() {
        return king;
    }



}
