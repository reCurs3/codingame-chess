package cc.recursive.chess;

import java.util.List;
import java.util.stream.Collectors;

public class ViewData {
    private int game;
    private int[] scores = new int[2];
    private String result;
    private String fen;
    private List<String> highlights;
    private String move;
    private String status;
    private String comment;

    public int getGame() { return game; }
    public void setGame(int game) { this.game = game; }

    public int[] getScores() { return scores; }
    public void setScores(int score1, int score2) {
        scores[0] = score1;
        scores[1] = score2;
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getFen() { return fen; }
    public void setBoard(Board board) { fen = board.toFenString(false); }

    public List<String> getHighlights() { return highlights; }
    public void setHighlights(Board board) {
        this.highlights = board.getHighlightSquares().stream().map(square -> square.toString()).collect(Collectors.toList());
    }

    public String getMove() { return move; }
    public void setMove(String move) {
        this.move = move;
    }
    public void setMove(Board board, Move move) {
        this.move = board.getAlgebraicMoveUnsafe(move);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
