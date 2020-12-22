import java.text.ParseException;
import java.util.List;

import cc.recursive.chess.Board;
import cc.recursive.chess.Move;

public class TestLegalMoves {
    private static int countLegalMoves(Board board, int depth) {
        List<Move> moves = board.generateMoves();
        if (depth == 1)
            return moves.size();

        int result = 0;
        for (Move move : moves) {
            Board newBoard = new Board(board);
            newBoard.applyMoveUnsafe(move);
            int count = countLegalMoves(newBoard, depth - 1);
            result += count;
        }
        return result;
    }

    private static void testPosition(String fen, int depth, int validCount) throws ParseException {
        Board board = new Board(fen, false);
        int count = countLegalMoves(board, depth);
        if (count != validCount)
            System.out.println(String.format("Failed test for board '%s' depth %d: expected %d moves, got %d instead.", fen, depth, validCount, count));
    }

    // Using test suite from https://gist.github.com/peterellisjones/8c46c28141c162d1d8a0f0badbc9cff9
    public static void main(String[] args) throws ParseException {
        testPosition("r6r/1b2k1bq/8/8/7B/8/8/R3K2R b AH - 3 2", 1, 8);
        testPosition("8/8/8/2k5/2pP4/8/B7/4K3 b - d3 5 3", 1, 8);
        testPosition("r1bqkbnr/pppppppp/n7/8/8/P7/1PPPPPPP/RNBQKBNR w AHah - 2 2", 1, 19);
        testPosition("r3k2r/p1pp1pb1/bn2Qnp1/2qPN3/1p2P3/2N5/PPPBBPPP/R3K2R b AHah - 3 2", 1, 5);
        testPosition("2kr3r/p1ppqpb1/bn2Qnp1/3PN3/1p2P3/2N5/PPPBBPPP/R3K2R b AH - 3 2", 1, 44);
        testPosition("rnb2k1r/pp1Pbppp/2p5/q7/2B5/8/PPPQNnPP/RNB1K2R w AH - 3 9", 1, 39);
        testPosition("2r5/3pk3/8/2P5/8/2K5/8/8 w - - 5 4", 1, 9);
        testPosition("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w AH - 1 8", 3, 62379);
        testPosition("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10", 3, 89890);
        testPosition("3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1", 6, 1134888);
        testPosition("8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1", 6, 1015133);
        testPosition("8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 6, 1440467);
        testPosition("5k2/8/8/8/8/8/8/4K2R w H - 0 1", 6, 661072);
        testPosition("3k4/8/8/8/8/8/8/R3K3 w A - 0 1", 6, 803711);
        testPosition("r3k2r/1b4bq/8/8/8/8/7B/R3K2R w AHah - 0 1", 4, 1274206);
        testPosition("r3k2r/8/3Q4/8/8/5q2/8/R3K2R b AHah - 0 1", 4, 1720476);
        testPosition("2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1", 6, 3821001);
        testPosition("8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1", 5, 1004658);
        testPosition("4k3/1P6/8/8/8/8/K7/8 w - - 0 1", 6, 217342);
        testPosition("8/P1k5/K7/8/8/8/8/8 w - - 0 1", 6, 92683);
        testPosition("K1k5/8/P7/8/8/8/8/8 w - - 0 1", 6, 2217);
        testPosition("8/k1P5/8/1K6/8/8/8/8 w - - 0 1", 7, 567584);
        testPosition("8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1", 4, 23527);
        System.out.println("Done testing");
    }
}
