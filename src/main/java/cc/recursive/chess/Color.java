package cc.recursive.chess;

import java.text.ParseException;

public enum Color {
    White,
    Black;

    public Color opposite() { return this == White ? Black : White; }
    public char toChar() { return this == White ? 'w' : 'b'; }

    public static Color fromChar(char c) throws ParseException {
        switch (c)
        {
            case 'w': return White;
            case 'b': return Black;
        }
        throw new ParseException(String.format("Character '%c' does not designate a color.", c), 0);
    }
}
