import java.text.ParseException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import cc.recursive.chess.Board;
import cc.recursive.chess.Move;

public class RepetitionAgent {
    private static Scanner in;

    public static void main(String[] args) throws ParseException {
        in = new Scanner(System.in);

        int constantsCount = in.nextInt();
        boolean crazyHouse = false;
        for (int i = 0; i < constantsCount; i++) {
            String name = in.next();
            String value = in.next();
            if (name.equals("crazyHouse"))
                crazyHouse = Integer.parseInt(value) == 1 ? true : false;
        }
        in.nextLine();
        System.out.println("fen");

        Random rng = new Random();
        Move repeatMove = null;
        while (true) {
            String input = in.nextLine();
            Board board = new Board(input, crazyHouse);
            List<Move> moves = board.generateMoves();

            Move move;
            if (repeatMove != null && moves.contains(repeatMove))
                move = repeatMove;
            else
                move = moves.get(rng.nextInt(moves.size()));
            repeatMove = new Move(move.getTo(), move.getFrom());
            System.out.println(move.toString());
        }
    }
}
