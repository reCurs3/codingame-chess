package cc.recursive.chess;

import java.text.ParseException;

public class Piece {
    private PieceType pieceType;
    private Color color;
    private boolean promoted;

    public Piece(PieceType pieceType, Color color) { this(pieceType, color, false); }
    public Piece(PieceType pieceType, Color color, boolean promoted) { this.pieceType = pieceType; this.color = color; this.promoted = promoted; }

    public PieceType getPieceType() { return pieceType; }
    public Color getColor() { return color; }
    public boolean isPromoted() { return promoted; }

    public Piece invert() { return new Piece(pieceType, color.opposite()); }
    public Piece promote() { return new Piece(pieceType, color, true); }

    public char toChar() { return pieceType.toChar(color); }
    public String toString() { return Character.toString(toChar()); }

    public static Piece fromChar(char c) throws ParseException {
        PieceType pieceType = PieceType.fromChar(c);
        Color color = Character.isUpperCase(c) ? Color.White : Color.Black;
        return new Piece(pieceType, color);
    }
}
