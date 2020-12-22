import java.text.ParseException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import cc.recursive.chess.Board;
import cc.recursive.chess.Move;

public class RandomAgent {
    private static Scanner in;

    public static void main(String[] args) throws ParseException {
        in = new Scanner(System.in);

        int constantsCount = in.nextInt();
        System.err.println(String.format("%d constants", constantsCount));
        boolean crazyHouse = false;
        for (int i = 0; i < constantsCount; i++) {
            String name = in.next();
            String value = in.next();
            System.err.println(String.format("%s = %s", name, value));
            if (name.equals("crazyHouse"))
                crazyHouse = Integer.parseInt(value) == 1 ? true : false;
        }
        in.nextLine();
        System.out.println("game score lastmove draw fen moves");

        Random rng = new Random();
        while (true) {
            int game = in.nextInt();
            System.err.println(String.format("Game = %d", game));
            int score1 = in.nextInt(), score2 = in.nextInt();
            System.err.println(String.format("Score = %d %d", score1, score2));
            in.nextLine();
            String lastMove = in.nextLine();
            System.err.println(String.format("Last move = %s", lastMove));
            boolean drawOffered = in.nextInt() != 0;
            System.err.println(String.format("Draw offered = %b", drawOffered));
            in.nextLine();
            String fen = in.nextLine();
            System.err.println(String.format("FEN = %s", fen));
            int moveCount = in.nextInt();
            System.err.println(String.format("%d moves", moveCount));
            for (int i = 0; i < moveCount; i++)
                System.err.println(in.next());
            in.nextLine();

            /*if (drawOffered && rng.nextInt(16) == 0) {
                System.out.println("draw");
                continue;
            }

            if (rng.nextInt(32) == 0) {
                System.out.println("resign");
                continue;
            }*/

            Board board = new Board(fen, crazyHouse);
            List<Move> moves = board.generateMoves();
            System.out.println(moves.get(rng.nextInt(moves.size())).toString() /*+ (rng.nextInt(4) == 0 ? "=" : "")*/ + " comment " + rng.nextInt(100));
            //System.out.println("random");
        }
    }
}
