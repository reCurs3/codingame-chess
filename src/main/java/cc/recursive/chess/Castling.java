package cc.recursive.chess;

public class Castling {
    // Initial rook column.
    private int column;
    // Whether this castling is allowed for each color.
    private boolean[] colorAllowed = new boolean[Constants.Colors];

    public Castling(int column, boolean allowed) {
        this.column = column;
        for (int i = 0; i < colorAllowed.length; i++)
            colorAllowed[i] = allowed;
    }

    public Castling(Castling other) {
        column = other.column;
        for (int i = 0; i < colorAllowed.length; i++)
            colorAllowed[i] = other.colorAllowed[i];
    }

    public int getColumn() { return column; }
    public boolean isAllowed(Color color) { return colorAllowed[color.ordinal()]; }
    public void setAllowed(Color color, boolean allowed) { colorAllowed[color.ordinal()] = allowed; }
}
