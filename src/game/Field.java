package game;

import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Field extends GridPane {
    private static final double fieldSize = 576.0;
    private Selection selection = new Selection();
    private Piece[][] pieces = new Piece[8][8];
    private Side playerSide = Side.HUMAN;
    private static final int maxDepth = 5;
    private Map<Integer, MoveBot> moves = new HashMap<>();
    private static int move = 0;
    // флаги
    private boolean mustJump = false;
    private boolean multipleJump = false;
    // множества шашек
    private Set<Piece> whitePieces = new HashSet<>();
    private Set<Piece> blackPieces = new HashSet<>();
    private Set<Piece> capturedPieces = new HashSet<>();

    Field() {
        Image blackPieceImg = new Image("img/blackPiece.png");
        Image whitePieceImg = new Image("img/whitePiece.png");
        this.setPrefSize(fieldSize, fieldSize);
        this.setLayoutX(38.0);
        this.setLayoutY(38.0);
        selection.setFitHeight(fieldSize / 8.0);
        selection.setFitWidth(fieldSize / 8.0);
        int i, j;
        for(i = 0; i < 8; i++) {
            this.getColumnConstraints().add(new ColumnConstraints(fieldSize / 8.0));
            this.getRowConstraints().add(new RowConstraints(fieldSize / 8.0));
        }
        for(i = 0; i < pieces.length; i++) {
            j = (i % 2 == 0) ? 1 : 0;
            while(j < pieces.length) {
                if (i < 3) {
                    pieces[i][j] = new Piece(Side.BOT, blackPieceImg, fieldSize, i, j);
                    this.add(pieces[i][j], j, i);
                    blackPieces.add(pieces[i][j]);
                }
                else if (i > 4) {
                    pieces[i][j] = new Piece(Side.HUMAN, whitePieceImg, fieldSize, i, j);
                    this.add(pieces[i][j], j, i);
                    whitePieces.add(pieces[i][j]);
                }
                j += 2;
            }
        }
        this.setOnMouseClicked((final MouseEvent click) -> {
            if (playerSide == Side.HUMAN) {
                int row = (int) (click.getY() * 8 / fieldSize);
                int col = (int) (click.getX() * 8 / fieldSize);
                if (squareContainsPiece(row, col) && !multipleJump)
                    selectPiece(pieces[row][col]);
                else move(row, col);
                winnerCheck();

            }
            else if (playerSide == Side.BOT){
                moveBot();
                winnerCheck();
            }
        });
    }

    //проверка на победу/проигрыш
    private void winnerCheck() {
        int playerPieces = 0;
        int botPieces = 0;
        int i,j;
        for (i = 0; i < pieces.length; i++) {
            for (j = 0; j < pieces.length; j++) {
                if (squareContainsPiece(i,j)) {
                    Piece piece = pieces[i][j];
                    if (piece.hasSide(Side.BOT)){
                        botPieces += 1;
                    }
                    if (piece.hasSide(Side.HUMAN)){
                        playerPieces += 1;
                    }
                }
            }
        }
        if (botPieces==0) {
            WinnerScreen screen = new WinnerScreen();
            screen.winnerScreen();

        }
        else if (playerPieces==0) {
            LoseScreen screen = new LoseScreen();
            screen.winnerScreen();
        }
    }


    // выделяет шашку, если она принадлежит игроку и это не череда прыжков
    private void selectPiece(Piece piece) {
        if (piece.hasSide(playerSide = Side.HUMAN)) {
            selection.target = piece;
            this.getChildren().remove(selection);
            this.add(selection, piece.col, piece.row);
        }
    }


    // проверяет, существует ли клетка поля с заданными координатами
    private boolean squareExists(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }


    // проверяет, содержит ли клетка шашку
    private boolean squareContainsPiece(int row, int col) {
        return pieces[row][col] != null;
    }









    // ход игрока
    private void move(int row, int col) {
        if (row % 2 != col % 2 && selection.isSet()) { // если это игровая клетка и выделена шашка
            int diffR = Math.abs(row - selection.target.row);
            int diffC = Math.abs(col - selection.target.col);
            if (diffR == 1 && diffC == 1) simpleMove(row, col);
            else if(diffR == 2 && diffC == 2) jump(row, col);
        }
    }


    //лучший ход бота
    private MoveBot bestMove(){
        ScanPiece[][] scan = new ScanPiece[8][8];
        int i,j;
        for (i = 0; i < pieces.length; i++) {
            for (j = 0; j < pieces.length; j++) {
                if ((pieces[i][j] != null)){
                    Piece piece = pieces[i][j];
                    ScanPiece scanPiece = piece.pieceToScan();
                    scan[i][j] = scanPiece;
                }
                else {
                    scan[i][j] = null;

                }
            }
        }
        moves.clear();
        move += 1;
        findMoves(scan, 0, 0, 0, 0, 0, Side.BOT);
        return getBestMove();
    }

    private int miniMax(ScanPiece[][] fieldScan){
        int i,j, c = 0;
        for (i = 0; i < fieldScan.length; i++) {
            for (j = 0; j < fieldScan.length; j++) {
                if (fieldScan[i][j] != null) {
                    ScanPiece piece = fieldScan[i][j];
                    if (piece.hasSide(Side.BOT)){
                        c += 1;
                    }
                    else if (piece.hasSide(Side.HUMAN)){
                        c -= 1;
                    }
                }
            }
        }
        return c;
    }

    private ScanPiece[][] copyField(ScanPiece[][] field) {
        int i,j;
        ScanPiece[][] copy = new ScanPiece[8][8];
        for (i = 0; i < field.length; i++) {
            for (j = 0; j < field.length; j++) {
                if ((field[i][j] != null)){
                    ScanPiece piece = field[i][j];
                    copy[i][j] = piece;
                }
                else {
                    copy[i][j] = null;

                }
            }
        }
        return copy;
    }



    private void findMoves(ScanPiece[][] scan, int saveRow, int saveCol,
                           int toRow, int toCol, int depth, Side side){
        int x, y, i , j, c;
        boolean moveNotJump = true;
        if (depth < maxDepth) {
            if (depth > 0) {
                for (i = 0; i < scan.length; i++){
                    for(j = 0; j < scan.length; j++){
                        if (scan[i][j] != null) {
                            ScanPiece pieceScan = scan[i][j];
                            if(canJumpForScan(pieceScan, Side.HUMAN, scan) && side == Side.HUMAN && pieceScan.hasSide(Side.HUMAN)){
                                moveNotJump = false;
                                findJump(scan, saveRow, saveCol,
                                        toRow, toCol, depth, Side.HUMAN, pieceScan);
                            }
                            else if(canJumpForScan(pieceScan, Side.BOT, scan) && side == Side.BOT && pieceScan.hasSide(Side.BOT)){
                                moveNotJump = false;
                                findJump(scan, saveRow, saveCol,
                                        toRow, toCol, depth, Side.BOT, pieceScan);
                            }
                        }
                    }
                }
            }
            if (moveNotJump) {
                for (x = 0; x < scan.length; x++) {
                    for (y = 0; y < scan.length; y++) {
                        if (scan[x][y] != null){
                            ScanPiece pieceScan = scan[x][y];
                            if(side == Side.BOT && pieceScan.hasSide(Side.BOT)){
                                if (depth == 0) {
                                    if (pieceScan.isKing()) {
                                        if (squareExists(x-1, y-1)  && scan[x-1][y-1] == null){
                                            ScanPiece[][] newScan = copyField(scan);
                                            ScanPiece newPieceScan = newScan[x][y];
                                            newScan[x][y] = null;
                                            newPieceScan.row = x-1;
                                            newPieceScan.col = y-1;
                                            newScan[x-1][y-1] = newPieceScan;
                                            findMoves(newScan, x, y,
                                                    x-1, y-1, depth+1, Side.HUMAN);
                                        }
                                        if (squareExists(x-1, y+1)  && scan[x-1][y+1] == null){
                                            ScanPiece[][] newScan = copyField(scan);
                                            ScanPiece newPieceScan = newScan[x][y];
                                            newScan[x][y] = null;
                                            newPieceScan.row = x-1;
                                            newPieceScan.col = y+1;
                                            newScan[x-1][y+1] = newPieceScan;
                                            findMoves(newScan, x, y,
                                                    x-1, y+1, depth+1, Side.HUMAN);

                                        }
                                    }
                                    if (squareExists(x+1,y+1) && scan[x+1][y+1] == null){
                                        ScanPiece[][] newScan = copyField(scan);
                                        ScanPiece newPieceScan = newScan[x][y];
                                        newScan[x][y] = null;
                                        newPieceScan.row = x+1;
                                        newPieceScan.col = y+1;
                                        pieceScan.tryToBecomeKing();
                                        newScan[x+1][y+1] = newPieceScan;
                                        findMoves(newScan, x, y,
                                                x+1, y+1, depth+1, Side.HUMAN);
                                    }
                                    if (squareExists(x+1, y-1) && scan[x+1][y-1] == null){
                                        ScanPiece[][] newScan = copyField(scan);
                                        ScanPiece newPieceScan = newScan[x][y];
                                        newScan[x][y] = null;
                                        newPieceScan.row = x+1;
                                        newPieceScan.col = y-1;
                                        pieceScan.tryToBecomeKing();
                                        newScan[x+1][y-1] = newPieceScan;
                                        findMoves(newScan, x, y,
                                                x+1, y-1, depth+1, Side.HUMAN);
                                    }
                                }
                                else {
                                    if (squareExists(x-1, y-1)  && scan[x-1][y-1] == null
                                            && pieceScan.isKing()){
                                        ScanPiece[][] newScan = copyField(scan);
                                        ScanPiece newPieceScan = newScan[x][y];
                                        newScan[x][y] = null;
                                        newPieceScan.row = x-1;
                                        newPieceScan.col = y-1;
                                        newScan[x-1][y-1] = newPieceScan;
                                        findMoves(newScan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                    }
                                    if (squareExists(x-1, y+1)  && scan[x-1][y+1] == null
                                            && pieceScan.isKing()){
                                        ScanPiece[][] newScan = copyField(scan);
                                        ScanPiece newPieceScan = newScan[x][y];
                                        newScan[x][y] = null;
                                        newPieceScan.row = x-1;
                                        newPieceScan.col = y+1;
                                        newScan[x-1][y+1] = newPieceScan;
                                        findMoves(newScan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                    }
                                    if (squareExists(x+1, y+1)  && scan[x+1][y+1] == null){
                                        ScanPiece[][] newScan = copyField(scan);
                                        ScanPiece newPieceScan = newScan[x][y];
                                        newScan[x][y] = null;
                                        newPieceScan.row = x+1;
                                        newPieceScan.col = y+1;
                                        newPieceScan.tryToBecomeKing();
                                        newScan[x+1][y+1] = newPieceScan;
                                        findMoves(newScan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                    }
                                    if (squareExists(x+1, y-1)  && scan[x+1][y-1] == null){
                                        ScanPiece[][] newScan = copyField(scan);
                                        ScanPiece newPieceScan = newScan[x][y];
                                        newScan[x][y] = null;
                                        newPieceScan.row = x+1;
                                        newPieceScan.col = y-1;
                                        newPieceScan.tryToBecomeKing();
                                        newScan[x+1][y-1] = newPieceScan;
                                        findMoves(newScan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                    }
                                }
                            }
                            else if(side == Side.HUMAN && pieceScan.hasSide(Side.HUMAN)
                                    && depth > 0) {
                                if (squareExists(x-1, y-1)  && scan[x-1][y-1] == null){
                                    ScanPiece[][] newScan = copyField(scan);
                                    ScanPiece newPieceScan = newScan[x][y];
                                    newScan[x][y] = null;
                                    newPieceScan.row = x-1;
                                    newPieceScan.col = y-1;
                                    newPieceScan.tryToBecomeKing();
                                    newScan[x-1][y-1] = newPieceScan;
                                    findMoves(newScan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                }
                                if (squareExists(x-1, y+1)  && scan[x-1][y+1] == null){
                                    ScanPiece[][] newScan = copyField(scan);
                                    ScanPiece newPieceScan = newScan[x][y];
                                    newScan[x][y] = null;
                                    newPieceScan.row = x-1;
                                    newPieceScan.col = y+1;
                                    newPieceScan.tryToBecomeKing();
                                    newScan[x-1][y+1] = newPieceScan;
                                    findMoves(newScan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                }
                                if (squareExists(x+1, y+1)  && scan[x+1][y+1] == null
                                        && pieceScan.isKing()){
                                    ScanPiece[][] newScan = copyField(scan);
                                    ScanPiece newPieceScan = newScan[x][y];
                                    newScan[x][y] = null;
                                    newPieceScan.row = x+1;
                                    newPieceScan.col = y+1;
                                    newScan[x+1][y+1] = newPieceScan;
                                    findMoves(newScan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                }
                                if (squareExists(x+1, y-1)  && scan[x+1][y-1] == null
                                        && pieceScan.isKing()){
                                    ScanPiece[][] newScan = copyField(scan);
                                    ScanPiece newPieceScan = newScan[x][y];
                                    newScan[x][y] = null;
                                    newPieceScan.row = x+1;
                                    newPieceScan.col = y-1;
                                    newScan[x+1][y-1] = newPieceScan;
                                    findMoves(newScan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                }
                            }
                        }
                    }
                }
            }

        }
        else if (depth == maxDepth) {
            if (squareExists(saveRow,saveCol) && squareExists(toRow, toCol)){
                MoveBot move = new MoveBot(saveRow, saveCol, toRow, toCol);
                Piece piece = pieces[saveRow][saveCol];
                c = miniMax(scan);
                if (piece.hasSide(Side.BOT) && pieces[toRow][toCol] == null
                        && avoidBackMove(saveRow, toRow, piece)){
                    if (!moves.containsKey(c)){
                        moves.put(c, move);
                    }
                }
            }
        }
    }

    private Side anotherSide(Side side){
        if (side == Side.HUMAN) {
            return Side.BOT;
        }
        else {
            return  Side.HUMAN;
        }
    }


    private boolean avoidBackMove(int saveRow, int toRow, Piece piece){
        int shiftRow = toRow - saveRow;
        if (shiftRow == -1 && !piece.isKing()) {
            return false;
        }
        return true;
    }

    private void findJump(ScanPiece[][] scan, int saveRow, int saveCol,
                          int toRow, int toCol, int depth, Side side, ScanPiece pieceScan) {
        int x = pieceScan.row;
        int y = pieceScan.col;
        if (canJumpForScan(pieceScan, side, scan)){
            if (squareExists(x+1, y+1) && scan[x+1][y+1] != null
                    && scan[x+1][y+1].hasSide(anotherSide(side)) && squareExists(x+2, y+2)
                    && scan[x+2][y+2] == null) {
                int capturedX = x+1;
                int capturedY = y+1;
                scan[capturedX][capturedY] = null;
                scan[x][y] = null;
                pieceScan.row = x+2;
                pieceScan.col = y+2;
                pieceScan.tryToBecomeKing();
                scan[x+2][y+2] = pieceScan;
                findJump(scan, saveRow, saveCol,
                        toRow, toCol, depth, side, pieceScan);

            }
            else if (squareExists(x+1, y-1) && scan[x+1][y-1] != null
                    && scan[x+1][y-1].hasSide(anotherSide(side)) && squareExists(x+2, y-2)
                    && scan[x+2][y-2] == null){
                int capturedX = x+1;
                int capturedY = y-1;
                scan[capturedX][capturedY] = null;
                scan[x][y] = null;
                pieceScan.row = x+2;
                pieceScan.col = y-2;
                pieceScan.tryToBecomeKing();
                scan[x+2][y-2] = pieceScan;
                findJump(scan, saveRow, saveCol,
                        toRow, toCol, depth, side, pieceScan);
            }
            else if (squareExists(x-1, y+1) && scan[x-1][y+1] != null
                    && scan[x-1][y+1].hasSide(anotherSide(side)) && squareExists(x-2, y+2)
                    && scan[x-2][y+2] == null) {
                int capturedX = x-1;
                int capturedY = y+1;
                scan[capturedX][capturedY] = null;
                scan[x][y] = null;
                pieceScan.row = x-2;
                pieceScan.col = y+2;
                pieceScan.tryToBecomeKing();
                scan[x-2][y+2] = pieceScan;
                findJump(scan, saveRow, saveCol,
                        toRow, toCol, depth, side, pieceScan);
            }
            else if (squareExists(x-1, y-1) && scan[x-1][y-1] != null
                    && scan[x-1][y-1].hasSide(anotherSide(side)) && squareExists(x-2, y-2)
                    && scan[x-2][y-2] == null) {
                int capturedX = x-1;
                int capturedY = y-1;
                scan[capturedX][capturedY] = null;
                scan[x][y] = null;
                pieceScan.row = x-2;
                pieceScan.col = y-2;
                pieceScan.tryToBecomeKing();
                scan[x-2][y-2] = pieceScan;
                findJump(scan, saveRow, saveCol,
                        toRow, toCol, depth, side, pieceScan);
            }
        }
        else {
            findMoves(scan, saveRow, saveCol,
                    toRow, toCol, depth+1, anotherSide(side));
        }
    }


    private MoveBot getBestMove(){
        int current = -999999;
        for (int i : moves.keySet()) {
            if(current < i){
                current = i;
            }
        }
        if (moves.keySet().isEmpty()) {
            return null;
        }
        else {return moves.get(current);}
    }



    // ход бота
    private void moveBot() {
        MoveBot black;
        if (mustJump) {
            int i, j;
            for (i = 0; i < pieces.length; i++) {
                for (j = 0; j < pieces.length; j++) {
                    if (squareContainsPiece(i,j)) {
                        Piece piece = pieces[i][j];
                        if (piece.hasSide(Side.BOT) && canJump(piece)){
                            jumpBot(piece);
                            break;
                        }
                    }
                }
            }
        }
        else {
            black = bestMove();
            winnerCheck();
            if (black == null){
                WinnerScreen screen = new WinnerScreen();
                screen.winnerScreen();
            }
            else if (squareContainsPiece(black.getRow(),black.getCol()) && squareExists(black.getRow(),black.getCol())) {
                Piece piece = pieces[black.getRow()][black.getCol()];
                movePiece(piece, black.getMoveToRow(), black.getMoveToCol());
                piece.tryToBecomeKing();
                switchPlayer();
            }
        }
    }

    private boolean checkHuman(int row, int col){
        Piece piece = pieces[row][col];
        return piece.hasSide(Side.HUMAN);
    }




    // прыжок шашки бота (через шашку человека)
    private void jumpBot(Piece piece) {
        int i = piece.getRow();
        int j = piece.getCol();
        if (squareExists(i+1, j+1) && squareContainsPiece(i+1, j+1)
                && checkHuman(i+1,j+1) && squareExists(i+2, j+2)
                && !squareContainsPiece(i+2, j+2)) {
            int capturedX = j+1;
            int capturedY = i+1;
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX];
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i+2, j+2);
                    piece.tryToBecomeKing();
                    if (canJump(piece)) {
                        jumpBot(piece);
                    } else {
                        removeCapturedPieces();
                        switchPlayer();
                    }
                }
            }
        }
        else if (squareExists(i+1, j-1) && squareContainsPiece(i+1, j-1)
                && checkHuman(i+1,j-1) && squareExists(i+2, j-2)
                && !squareContainsPiece(i+2, j-2)) {
            int capturedX = j-1;
            int capturedY = i+1;
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX];
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i+2, j-2);
                    piece.tryToBecomeKing();
                    if (canJump(piece)) {
                        jumpBot(piece);
                    } else {
                        removeCapturedPieces();
                        switchPlayer();
                    }
                }
            }
        }
        else if (squareExists(i-1, j+1) && squareContainsPiece(i-1, j+1)
                && checkHuman(i-1,j+1) && squareExists(i-2, j+2)
                && !squareContainsPiece(i-2, j+2)) {
            int capturedX = j+1;
            int capturedY = i-1;
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX];

                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i-2, j+2);
                    piece.tryToBecomeKing();
                    if (canJump(piece)) {
                        jumpBot(piece);
                    } else {
                        removeCapturedPieces();
                        switchPlayer();
                    }
                }
            }
        }
        else if (squareExists(i-1, j-1) && squareContainsPiece(i-1, j-1)
                && checkHuman(i-1,j-1) && squareExists(i-2, j-2)
                && !squareContainsPiece(i-2, j-2)) {
            int capturedX = j-1;
            int capturedY = i-1;
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX];
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i-2, j-2);
                    piece.tryToBecomeKing();
                    if (canJump(piece)) {
                        jumpBot(piece);
                    } else {
                        removeCapturedPieces();
                        switchPlayer();
                    }
                }
            }
        }
    }







    // простой ход (перемещение на одну клетку)
    private void simpleMove(int row, int col) {
        if (!mustJump) {
            Piece piece = selection.target;
            boolean moveBack = (playerSide == Side.BOT) != (row - piece.row > 0);
            if(!moveBack || piece.isKing()) {
                movePiece(piece, row, col);
                piece.tryToBecomeKing();
                switchPlayer();
            }
        }
    }


    // перемещение шашки
    private void movePiece(Piece piece, int row, int col) {
        pieces[piece.row][piece.col] = null;
        pieces[row][col] = piece;
        this.getChildren().remove(piece);
        this.add(piece, col, row);
        piece.row = row;
        piece.col = col;
    }


    // удаление с доски захваченных шашек
    private void removeCapturedPieces() {
        for(Piece captured : capturedPieces) {
            pieces[captured.row][captured.col] = null;
            if(playerSide == Side.HUMAN) blackPieces.remove(captured);
            else whitePieces.remove(captured);
            this.getChildren().remove(captured);
        }
    }


    // прыжок шашкой (через шашку соперника)
    private void jump(int row, int col) {
        if (playerSide == Side.HUMAN) {
            Piece piece = selection.target;
            int capturedX = piece.col + (col - piece.col) / 2; // координаты
            int capturedY = piece.row + (row - piece.row) / 2; // захватываемой шашки
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    movePiece(piece, row, col); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
                    if (canJump(piece)) {
                        selectPiece(piece);
                        multipleJump = true;
                    } else {
                        removeCapturedPieces();
                        multipleJump = false;
                        switchPlayer();
                    }
                }
            }
        }
    }

    private boolean canJump(Piece piece) {
        int rowShift = 1, colShift = 1, row, col;
        for(int i = 0, c = 1; i < 4; i++, c *= (-1)) {
            rowShift *= c;
            colShift *= -c;
            row = piece.row + rowShift;
            col = piece.col + colShift;
            if(squareExists(row, col) && squareContainsPiece(row, col)
                    && !pieces[row][col].hasSide(playerSide)
                    && !capturedPieces.contains(pieces[row][col])) {
                row += rowShift;
                col += colShift;
                if(squareExists(row, col) && !squareContainsPiece(row, col)) return true;
            }
        }
        return false;
    }


    private boolean canJumpForScan(ScanPiece piece, Side side, ScanPiece[][] scan) {
        int rowShift = 1, colShift = 1, row, col;
        for(int i = 0, c = 1; i < 4; i++, c *= (-1)) {
            rowShift *= c;
            colShift *= -c;
            row = piece.row + rowShift;
            col = piece.col + colShift;
            if(squareExists(row, col) && scan[row][col] != null
                    && scan[row][col].hasSide(anotherSide(side))) {
                row += rowShift;
                col += colShift;
                if(squareExists(row, col) && scan[row][col] == null) return true;
            }
        }
        return false;
    }




    // переключение игрока (передача хода другому игроку)
    private void switchPlayer() {
        playerSide = playerSide == Side.HUMAN ? Side.BOT : Side.HUMAN;// смена стороны
        this.getChildren().remove(selection);// сброс выделения
        selection.target = null;
        capturedPieces.clear(); // очистка множества захваченных шашек
        Set<Piece> playerPieces = playerSide == Side.HUMAN ? whitePieces : blackPieces;
        mustJump = false;
        for(Piece piece : playerPieces) { // для всех шашек игрока
            if(canJump(piece)) { // если шашка может есть
                mustJump = true; // флаг "обязан есть"
                break; // завершение проверки
            }
        }
    }
}