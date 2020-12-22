package cc.recursive.chess;

public class Constants {
    public static final int Columns = 8;
    public static final int Rows = 8;
    public static final int RangeLimit = 7;
    public static final int Colors = 2;
    public static final int Castlings = 2;

    public static final int QueenCastlingKingColumn = 2;
    public static final int QueenCastlingRookColumn = 3;
    public static final int KingCastlingKingColumn = 6;
    public static final int KingCastlingRookColumn = 5;
    public static final int KnightMoves = 8;
    public static final int[] KnightDeltaColumn = {-1,  1, -2,  2, -2,  2, -1,  1};
    public static final int[] KnightDeltaRow    = {-2, -2, -1, -1,  1,  1,  2,  2};

    public static final PieceType[] ReservePieces = { PieceType.Pawn, PieceType.Knight, PieceType.Bishop, PieceType.Rook, PieceType.Queen };

    public static final int MaxMovesPerGame = 125;
    public static final int MaxCommentLength = 30;
}
