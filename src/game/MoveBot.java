package game;

public class MoveBot{
    private int row, col, moveToRow, moveToCol;


    MoveBot(int row, int col, int moveToRow, int moveToCol) {
        this.row = row;
        this.col = col;
        this.moveToCol = moveToCol;
        this.moveToRow = moveToRow;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getMoveToCol() {
        return moveToCol;
    }

    public int getMoveToRow() {
        return moveToRow;
    }





}
