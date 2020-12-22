package cc.recursive.chess;

import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;

public class Move {
    // If null, move is a drop.
    private Square from = null;
    private Square to = null;
    // Used for promotion or drop.
    private Optional<PieceType> pieceType = Optional.empty();

    public Move(Square from, Square to) {
        this.from = from;
        this.to = to;
    }
    public Move(Square from, Square to, PieceType pieceType) {
        this(from, to);
        this.pieceType = Optional.of(pieceType);
    }

    // Parse from UCI representation. https://en.wikipedia.org/wiki/Universal_Chess_Interface
    public Move(String str) throws ParseException {
        if (str.length() < 4) throw new ParseException("Move string needs to be at least 4 characters.", 0);
        if (str.charAt(1) == '@') {
            to = new Square(str.substring(2, 4));
            pieceType = Optional.of(PieceType.fromChar(str.charAt(0)));
            if (pieceType.get() == PieceType.King) throw new ParseException("Drop piece is invalid.", 0);
            return;
        }
        from = new Square(str.substring(0, 2));
        to = new Square(str.substring(2, 4));
        if (str.length() == 5) {
            PieceType pieceType = PieceType.fromChar(str.charAt(4));
            if (pieceType == PieceType.Pawn || pieceType == PieceType.King) throw new ParseException("Promotion piece is invalid.", 0);
            this.pieceType = Optional.of(pieceType);
        }
    }

    public Square getFrom() { return from; }
    public Square getTo() { return to; }
    public Optional<PieceType> getPieceType() { return pieceType; }

    // Encode move to UCI representation. https://en.wikipedia.org/wiki/Universal_Chess_Interface
    public String toString() throws IllegalStateException {
        if (to == null) throw new IllegalStateException("Move has no destination square.");
        if (from == null) {
            if (!pieceType.isPresent()) throw new IllegalStateException("Drop move has no piece type.");
            return pieceType.get().toString() + "@" + to.toString();
        }
        String result = from.toString() + to.toString();
        if (pieceType.isPresent())
            result += Character.toString(Character.toLowerCase(pieceType.get().toChar()));
        return result;
    }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move other = (Move)o;
        return Objects.equals(from, other.from) && Objects.equals(to, other.to) && Objects.equals(pieceType, other.pieceType);
    }
    
    @Override public int hashCode() {
        return Objects.hash(from, to, pieceType);
    }
}
