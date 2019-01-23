package game;

import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.util.HashSet;
import java.util.Set;

class Field extends GridPane {
    private static final double fieldSize = 576.0;
    private Selection selection = new Selection();
    private Piece[][] pieces = new Piece[8][8];
    private Side playerSide = Side.HUMAN;
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
            }
            else {
                moveBot();
            }
        });
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


    // ход ИИ
    private void moveBot() {
        Bot black;
        if (mustJump) {
            black = new Bot();
            System.out.println("I must eat");
            int i, j;
            for (i = 0; i < pieces.length; i++) {
                for (j = 0; j < pieces.length; j++) {
                    if (squareContainsPiece(i,j)) {
                        Piece piece = pieces[i][j];
                        if (piece.hasSide(Side.BOT) && canJump(piece)){
                            jumpBot(piece);
                            System.out.println("1");
                            break;
                        }
                    }
                }
            }
        }
        else {
            black = new Bot();
            black.moveBot();
            System.out.println("I am thinking");
            switchPlayer();
        }
    }

    private boolean checkHuman(int row, int col){
        Piece piece = pieces[row][col];
        return piece.hasSide(Side.HUMAN);
    }



    private void jumpBot(Piece piece) {
        int i = piece.getRow();
        int j = piece.getCol();
        if (squareExists(i-1, j-1) && squareContainsPiece(i-1, j-1)
                && checkHuman(i-1,j-1)) {
            int capturedX = j-1; // координаты
            int capturedY = i-1; // захватываемой шашки
            System.out.println("1");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    movePiece(piece, i-2, j-2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
                    if (canJump(piece)) {
                        removeCapturedPieces();
                        jumpBot(piece);
                        multipleJump = true;
                    } else {
                        removeCapturedPieces();
                        multipleJump = false;
                        switchPlayer();
                    }
                }
            }}
        else if (squareExists(i+1, j+1) && squareContainsPiece(i+1, j+1)
                && checkHuman(i+1,j+1)) {
            int capturedX = j+1; // координаты
            int capturedY = i+1; // захватываемой шашки
            System.out.println("2");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    movePiece(piece, i+2, j+2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
                    if (canJump(piece)) {
                        removeCapturedPieces();
                        jumpBot(piece);
                        multipleJump = true;
                    } else {
                        removeCapturedPieces();
                        multipleJump = false;
                        switchPlayer();
                    }
                }
            }}
        else if (squareExists(i+1, j-1) && squareContainsPiece(i+1, j-1)
                && checkHuman(i+1,j-1)) {
            int capturedX = j-1; // координаты
            int capturedY = i+1; // захватываемой шашки
            System.out.println("3");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    movePiece(piece, i+2, j-2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
                    if (canJump(piece)) {
                        removeCapturedPieces();
                        jumpBot(piece);
                        multipleJump = true;
                    } else {
                        removeCapturedPieces();
                        multipleJump = false;
                        switchPlayer();
                    }
                }
            }}
        else if (squareExists(i-1, j+1) && squareContainsPiece(i-1, j+1)
                && checkHuman(i-1,j+1)) {
            int capturedX = j+1; // координаты
            int capturedY = i-1; // захватываемой шашки
            System.out.println("4");
            if(squareContainsPiece(capturedY, capturedX)) {
                Piece captured = pieces[capturedY][capturedX]; // сама захватываемая шашка
                // если захватыв. шашка приндл. сопернику и ещё не была захвачена (на этом ходу)
                if (!captured.hasSide(playerSide) && capturedPieces.add(captured)) {
                    movePiece(piece, i-2, j+2); // перемещение шашки
                    piece.tryToBecomeKing(); // шашка становится дамкой, если дошла до конца поля
                    if (canJump(piece)) {
                        removeCapturedPieces();
                        jumpBot(piece);
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