package cc.recursive.chess;

import java.text.ParseException;

public enum PieceType {
    Pawn,
    Knight,
    Bishop,
    Rook,
    Queen,
    King;

    public char toChar() {
        switch (this) {
            case Pawn:   return 'P';
            case Knight: return 'N';
            case Bishop: return 'B';
            case Rook:   return 'R';
            case Queen:  return 'Q';
            case King:   return 'K';
        }
        throw new IllegalStateException("Unrecognized piece type.");
    }

    public char toChar(Color color) {
        char c = toChar();
        if (color == Color.Black) c = Character.toLowerCase(c);
        return c;
    }

    public String toString() { return Character.toString(toChar()); }

    public static PieceType fromChar(char c) throws ParseException {
        switch (Character.toUpperCase(c)) {
            case 'P': return Pawn;
            case 'N': return Knight;
            case 'B': return Bishop;
            case 'R': return Rook;
            case 'Q': return Queen;
            case 'K': return King;
        }
        throw new ParseException(String.format("Character '%c' does not designate a piece.", c), 0);
    }
}
