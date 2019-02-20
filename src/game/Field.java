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
    private ScanPiece[][] scan = new ScanPiece[8][8];
    private Side playerSide = Side.HUMAN;
    private static final int maxDepth = 4;
    private Map<Integer, MoveBot> moves = new HashMap<>();
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
        boolean checkPlayer = true;
        boolean checkBot = true;
        int i,j;
        for (i = 0; i < pieces.length; i++) {
            for (j = 0; j < pieces.length; j++) {
                if (squareContainsPiece(i,j)) {
                    Piece piece = pieces[i][j];
                    if (piece.hasSide(Side.BOT)){
                        checkPlayer = false;
                    }
                    if (piece.hasSide(Side.HUMAN)){
                        checkBot = false;
                    }
                }
            }
        }
        if (checkPlayer) {
            WinnerScreen screen = new WinnerScreen();
            screen.winnerScreen();

        }
        if (checkBot) {
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
        int i,j;
        for (i = 0; i < pieces.length; i++) {
            for (j = 0; j < pieces.length; j++) {
                if ((squareContainsPiece(i,j))){
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
        findMoves(0 ,scan, 0, 0, 0, 0, 0, Side.BOT);
        return getBestMove();
    }





    private void findMoves(int c, ScanPiece[][] scan, int saveRow, int saveCol,
                           int toRow, int toCol, int depth, Side side){
        int x, y, i , j;
        boolean moveNotJump = true;
        if (depth < maxDepth) {
            for (i = 0; i < scan.length; i++){
                for(j = 0; j < scan.length; j++){
                    if (scan[i][j] != null) {
                        ScanPiece pieceScan = scan[i][j];
                        if(side == Side.HUMAN && pieceScan.hasSide(Side.HUMAN) && depth > 0){
                            // в этом фрагменте происходит с-1
                            //System.out.println(depth + " глубина, шашка(игрок): строка " + i +" столбец "+ j);
                            if (canJumpForBot(pieceScan)){
                                moveNotJump = false;
                                //musteat-HUMAN
                                findJump(c ,scan, saveRow, saveCol,
                                        toRow, toCol, depth, Side.HUMAN, pieceScan);

                            }
                        }
                        else if(side == Side.BOT && pieceScan.hasSide(Side.BOT)){
                            // в этом фрагменте происходит c+1
                            //System.out.println(depth + " глубина, шашка(бот): строка " + i +" столбец "+ j);
                            if (canJumpForHuman(pieceScan)){
                                moveNotJump = false;
                                findJump(c ,scan, saveRow, saveCol,
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
                                // в этом фрагменте происходит c+1
                                //System.out.println(depth + " глубина, шашка: строка " + x +" столбец "+ y);
                                if (depth == 0) {
                                    if (squareExists(x-1, y-1)  && scan[x-1][y-1] == null
                                            && pieceScan.isKing()){
                                        moveScanPiece(pieceScan, x-1, y-1, scan);
                                        findMoves(c ,scan, x, y,
                                                x-1, y-1, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                    }
                                    if (squareExists(x-1, y+1)  && scan[x-1][y+1] == null
                                            && pieceScan.isKing()){
                                        moveScanPiece(pieceScan, x-1, y+1, scan);
                                        findMoves(c ,scan, x, y,
                                                x-1, y+1, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                    }
                                    if (squareExists(x+1,y+1) && scan[x+1][y+1] == null){
                                        moveScanPiece(pieceScan, x+1, y+1, scan);
                                        boolean notKing = false;
                                        if (pieceScan.isKing()) {notKing = true;}
                                        pieceScan.tryToBecomeKing();
                                        //System.out.println("ход 1: " + "строка " + (x+1) + " столбец " + (y+1));
                                        findMoves(c ,scan, x, y,
                                                x+1, y+1, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                        if (notKing) {pieceScan.becomeNormal();}
                                    }
                                    if (squareExists(x+1, y-1) && scan[x+1][y-1] == null){
                                        moveScanPiece(pieceScan, x+1, y-1, scan);
                                        boolean notKing = false;
                                        if (pieceScan.isKing()) {notKing = true;}
                                        pieceScan.tryToBecomeKing();
                                        //System.out.println("ход 2: " + "строка " + (x+1) + " столбец " + (y-1));
                                        findMoves(c ,scan, x, y,
                                                x+1, y-1, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                        if (notKing) {pieceScan.becomeNormal();}
                                    }
                                }
                                else if((depth%2)==0) {
                                    if (squareExists(x-1, y-1)  && scan[x-1][y-1] == null
                                            && pieceScan.isKing()){
                                        moveScanPiece(pieceScan, x-1, y-1, scan);
                                        findMoves(c ,scan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                    }
                                    if (squareExists(x-1, y+1)  && scan[x-1][y+1] == null
                                            && pieceScan.isKing()){
                                        moveScanPiece(pieceScan, x-1, y+1, scan);
                                        findMoves(c ,scan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                    }
                                    if (squareExists(x+1, y+1)  && scan[x+1][y+1] == null){
                                        moveScanPiece(pieceScan, x+1, y+1, scan);
                                        boolean notKing = false;
                                        if (pieceScan.isKing()) {notKing = true;}
                                        pieceScan.tryToBecomeKing();
                                        findMoves(c ,scan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                        if (notKing) {pieceScan.becomeNormal();}
                                    }
                                    if (squareExists(x+1, y-1)  && scan[x+1][y-1] == null){
                                        moveScanPiece(pieceScan, x+1, y-1, scan);
                                        boolean notKing = false;
                                        if (pieceScan.isKing()) {notKing = true;}
                                        pieceScan.tryToBecomeKing();
                                        findMoves(c ,scan, saveRow, saveCol,
                                                toRow, toCol, depth+1, Side.HUMAN);
                                        moveScanPiece(pieceScan, x, y, scan);
                                        if (notKing) {pieceScan.becomeNormal();}
                                    }
                                }
                            }
                            else if(side == Side.HUMAN && pieceScan.hasSide(Side.HUMAN)
                                    && depth > 0 && (depth%2)==1){
                                // в этом фрагменте происходит с-1
                                if (squareExists(x-1, y-1)  && scan[x-1][y-1] == null){
                                    moveScanPiece(pieceScan, x-1, y-1, scan);
                                    boolean notKing = false;
                                    if (pieceScan.isKing()) {notKing = true;}
                                    pieceScan.tryToBecomeKing();
                                    findMoves(c ,scan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                    moveScanPiece(pieceScan, x, y, scan);
                                    if (notKing) {pieceScan.becomeNormal();}
                                }
                                if (squareExists(x-1, y+1)  && scan[x-1][y+1] == null){
                                    moveScanPiece(pieceScan, x-1, y+1, scan);
                                    boolean notKing = false;
                                    if (pieceScan.isKing()) {notKing = true;}
                                    pieceScan.tryToBecomeKing();
                                    findMoves(c ,scan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                    moveScanPiece(pieceScan, x, y, scan);
                                    if (notKing) {pieceScan.becomeNormal();}
                                }
                                if (squareExists(x+1, y+1)  && scan[x+1][y+1] == null
                                        && pieceScan.isKing()){
                                    moveScanPiece(pieceScan, x+1, y+1, scan);
                                    findMoves(c ,scan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                    moveScanPiece(pieceScan, x, y, scan);
                                }
                                if (squareExists(x+1, y-1)  && scan[x+1][y-1] == null
                                        && pieceScan.isKing()){
                                    moveScanPiece(pieceScan, x+1, y-1, scan);
                                    findMoves(c ,scan, saveRow, saveCol,
                                            toRow, toCol, depth+1, Side.BOT);
                                    moveScanPiece(pieceScan, x, y, scan);
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
                //System.out.println("||ОБРАБОТКА|| Шашка " + saveRow + " " + saveCol + " ---> " + toRow + " " + toCol + " коэффициент = " + c);
                Piece piece = pieces[saveRow][saveCol];
                if (piece.hasSide(Side.BOT) && pieces[toRow][toCol] == null){
                    if (moves.containsKey(c) ){
                        moves.remove(c);
                    }
                    System.out.println("||ЗАПИСЬ|| Шашка " + saveRow + " " + saveCol + " ---> " + toRow + " " + toCol + " коэффициент = " + c);
                    moves.put(c, move);
                }
            }
        }
    }


    private void findJump(int c, ScanPiece[][] scan, int saveRow, int saveCol,
                          int toRow, int toCol, int depth, Side side, ScanPiece pieceScan) {
        int x = pieceScan.row;
        int y = pieceScan.col;
        if(side == Side.BOT && pieceScan.hasSide(Side.BOT)){
            if (canJumpForHuman(pieceScan)){
                //System.out.println(depth + " глубина, шашка(бот): строка " + x +" столбец "+ y);
                if (squareExists(x+1, y+1) && scan[x+1][y+1] != null
                        && scan[x+1][y+1].hasSide(Side.HUMAN) && squareExists(x+2, y+2)
                        && scan[x+2][y+2] == null) {
                    int capturedX = x+1;
                    int capturedY = y+1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x+2, y+2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 1");
                    findJump(c+1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.BOT, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;

                }
                else if (squareExists(x+1, y-1) && scan[x+1][y-1] != null
                        && scan[x+1][y-1].hasSide(Side.HUMAN) && squareExists(x+2, y-2)
                        && scan[x+2][y-2] == null){
                    int capturedX = x+1;
                    int capturedY = y-1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x+2, y-2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 2");
                    findJump(c+1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.BOT, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
                else if (squareExists(x-1, y+1) && scan[x-1][y+1] != null
                        && scan[x-1][y+1].hasSide(Side.HUMAN) && squareExists(x-2, y+2)
                        && scan[x-2][y+2] == null) {
                    int capturedX = x-1;
                    int capturedY = y+1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x-2, y+2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 3");
                    findJump(c+1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.BOT, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
                else if (squareExists(x-1, y-1) && scan[x-1][y-1] != null
                        && scan[x-1][y-1].hasSide(Side.HUMAN) && squareExists(x-2, y-2)
                        && scan[x-2][y-2] == null) {
                    int capturedX = x-1;
                    int capturedY = y-1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x-2, y-2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 4");
                    findJump(c+1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.BOT, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
            }
            else {
                findMoves(c ,scan, saveRow, saveCol,
                        toRow, toCol, depth+1, Side.HUMAN);
            }
        }
        else if(side == Side.HUMAN && pieceScan.hasSide(Side.HUMAN)){
            if (canJumpForBot(pieceScan)){
                //System.out.println(depth + " глубина, шашка(игрок): строка " + x +" столбец "+ y);
                if (squareExists(x+1, y+1) && scan[x+1][y+1] != null
                        && scan[x+1][y+1].hasSide(Side.BOT) && squareExists(x+2, y+2)
                        && scan[x+2][y+2] == null) {
                    int capturedX = x+1;
                    int capturedY = y+1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x+2, y+2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 1");
                    findMoves(c-1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.HUMAN);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
                if (squareExists(x+1, y-1) && scan[x+1][y-1] != null
                        && scan[x+1][y-1].hasSide(Side.BOT) && squareExists(x+2, y-2)
                        && scan[x+2][y-2] == null){
                    int capturedX = x+1;
                    int capturedY = y-1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x+2, y-2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 2");
                    findJump(c-1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.HUMAN, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
                if (squareExists(x-1, y+1) && scan[x-1][y+1] != null
                        && scan[x-1][y+1].hasSide(Side.BOT) && squareExists(x-2, y+2)
                        && scan[x-2][y+2] == null) {
                    int capturedX = x-1;
                    int capturedY = y+1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x-2, y+2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 3");
                    findJump(c-1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.HUMAN, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
                if (squareExists(x-1, y-1) && scan[x-1][y-1] != null
                        && scan[x-1][y-1].hasSide(Side.BOT) && squareExists(x-2, y-2)
                        && scan[x-2][y-2] == null) {
                    int capturedX = x-1;
                    int capturedY = y-1;
                    ScanPiece capturedScan = scan[capturedX][capturedY];
                    moveScanPiece(pieceScan, x-2, y-2, scan);
                    scan[capturedX][capturedY] = null;
                    //System.out.println("ест 4");
                    findJump(c-1 ,scan, saveRow, saveCol,
                            toRow, toCol, depth, Side.HUMAN, pieceScan);
                    // шаг назад
                    moveScanPiece(pieceScan, x, y, scan);
                    scan[pieceScan.row][pieceScan.col] = capturedScan;
                }
            }
            else {
                findMoves(c ,scan, saveRow, saveCol,
                        toRow, toCol, depth+1, Side.BOT);
            }
        }
    }


    private MoveBot getBestMove(){
        int current = -999999;
        for (int i : moves.keySet()) {
            if(current < i){
                current = i;
                System.out.println(i + " i");
            }
        }
        return moves.get(current);
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
                            System.out.println("1");
                            jumpBot(piece);
                            break;
                        }
                    }
                }
            }
        }
        else {
            black = bestMove();
            int row = black.getRow();
            int col = black.getCol();
            int toRow = black.getMoveToRow();
            int toCol = black.getMoveToCol();
            Piece piece = pieces[row][col];
            movePiece(piece, toRow, toCol);
            piece.tryToBecomeKing();
            switchPlayer();
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
            System.out.println("2");
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
            int capturedX = j-1; // координаты
            int capturedY = i+1; // захватываемой шашки
            System.out.println("3");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i+2, j-2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
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
            int capturedX = j+1; // координаты
            int capturedY = i-1; // захватываемой шашки
            System.out.println("4");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i-2, j+2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
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
            int capturedX = j-1; // координаты
            int capturedY = i-1; // захватываемой шашки
            System.out.println("1");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    removeCapturedPieces();
                    movePiece(piece, i-2, j-2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
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

    private void moveScanPiece(ScanPiece piece, int row, int col, ScanPiece[][] field) {
        field[piece.row][piece.col] = null;
        field[row][col] = piece;
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


    private boolean canJumpForBot(ScanPiece piece) {
        int rowShift = 1, colShift = 1, row, col;
        for(int i = 0, c = 1; i < 4; i++, c *= (-1)) {
            rowShift *= c;
            colShift *= -c;
            row = piece.row + rowShift;
            col = piece.col + colShift;
            if(squareExists(row, col) && scan[row][col] != null
                    && scan[row][col].hasSide(playerSide = Side.BOT)) {
                row += rowShift;
                col += colShift;
                if(squareExists(row, col) && scan[row][col] == null) return true;
            }
        }
        return false;
    }

    private boolean canJumpForHuman(ScanPiece piece) {
        int rowShift = 1, colShift = 1, row, col;
        for(int i = 0, c = 1; i < 4; i++, c *= (-1)) {
            rowShift *= c;
            colShift *= -c;
            row = piece.row + rowShift;
            col = piece.col + colShift;
            if(squareExists(row, col) && scan[row][col] != null
                    && scan[row][col].hasSide(Side.HUMAN)) {
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