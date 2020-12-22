package cc.recursive.chess;

import java.text.ParseException;

public class Square {
    private int column = 0, row = 0;

    public Square(int column, int row) { this.column = column; this.row = row; }
    public Square(String str) throws ParseException {
        if (str.length() != 2) throw new ParseException(String.format("'%s' is not a square.", str), 0);
        column = charToColumn(str.charAt(0));
        row = charToRow(str.charAt(1));
    }

    public int getColumn()   { return column; }
    public int getRow()      { return row; }

    // Returns whether this square is a light or dark square.
    public boolean isLight() { return ((column & 1) != 0) ^ ((row & 1) != 0); }

    // Returns a new square offset by the given directions or null if invalid.
    public Square offset(int deltaColumn, int deltaRow) {
        int newColumn = column + deltaColumn;
        int newRow = row + deltaRow;
        if (newColumn < 0 || newColumn >= Constants.Columns || newRow < 0 || newRow >= Constants.Rows)
            return null;
        return new Square(newColumn, newRow);
    }

    @Override public String toString() { return Character.toString(columnToChar(column)) + rowToChar(row); }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Square other = (Square)o;
        return column == other.column && row == other.row;
    }
    @Override public int hashCode() { return row * 8 + column; }

    public static char columnToChar(int column) { return (char)('a' + column); }

    public static char rowToChar(int row) { return (char)('1' + row); }

    public static int charToColumn(char c) throws ParseException {
        c = Character.toLowerCase(c);
        if (c < 'a' || c > 'h') throw new ParseException(String.format("Character '%c' is not a column.", c), 0);
        return c - 'a';
    }

    public static int charToRow(char c) throws ParseException {
        if (c < '1' || c > '8') throw new ParseException(String.format("Character '%c' is not a row.", c), 0);
        return c - '1';
    }
}
