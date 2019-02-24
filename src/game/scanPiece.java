package game;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ScanPiece{
    int row;
    int col;
    private Side side;
    private int lastRow;
    private boolean king;

    ScanPiece(Side _side, int _row, int _col, boolean isKing) {
        this.side = _side;
        lastRow = side == Side.HUMAN ? 0 : 7;
        row = _row;
        col = _col;
        king = isKing;
    }


    public boolean hasSide(Side _side) {
        return side == _side;
    }
    public boolean isKing() {
        return king;
    }

    public void tryToBecomeKing() {
        if(row == lastRow) {
            king = true;
        }
    }




}
