package cc.recursive.chess;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private Board board;
    private List<String> positionHistory = new ArrayList<String>();
    private GameResult gameResult = GameResult.Undecided;

    public Game(long seed, boolean crazyHouse) {
        board = new Board(seed, crazyHouse);
        positionHistory.add(board.toFenString(true));
    }

    public Board getBoard() { return board; }

    public ViewData applyMove(Move move) throws GameException {
        List<Move> moves = board.generateMoves();
        if (!moves.contains(move)) throw new GameException(String.format("'%s' is not a legal move", move.toString()));

        ViewData viewData = new ViewData();
        viewData.setMove(board, move);

        board.applyMoveUnsafe(move);
        positionHistory.add(board.toFenString(true));

        viewData.setBoard(board);
        viewData.setHighlights(board);
        return viewData;
    }

    public ViewData getViewData() {
        ViewData viewData = new ViewData();
        viewData.setBoard(board);
        return viewData;
    }

    public GameResult getGameResult() {
        if (gameResult == GameResult.Undecided)
            gameResult = computeGameResult();
        return gameResult;
    }

    public void setGameResult(GameResult gameResult) { this.gameResult = gameResult; }

    private GameResult computeGameResult() {
        // No move possible means checkmate or stalemate.
        if (board.generateMoves().size() == 0) {
            if (board.isKingAttacked(board.getColorToPlay()))
                return board.getColorToPlay() == Color.White ? GameResult.BlackWins : GameResult.WhiteWins;
            return GameResult.Stalemate;
        }

        // Check for threefold repetition.
        if (positionHistory.size() > 0) {
            int count = 0;
            String lastPosition = positionHistory.get(positionHistory.size() - 1);
            for (int i = 0; i < positionHistory.size() - 1; i++) {
                if (!positionHistory.get(i).equals(lastPosition)) continue;
                count++;
                if (count == 2)
                    return GameResult.Repetition;
            }
        }

        if (!board.hasSufficientMaterial())
            return GameResult.InsufficientMaterial;

        if (board.getHalfMoveClock() >= 100)
            return GameResult.FiftyMove;

        if (board.getHalfMoves() >= Constants.MaxMovesPerGame * 2)
            return GameResult.ForcedDraw;

        return GameResult.Undecided;
    }
}
