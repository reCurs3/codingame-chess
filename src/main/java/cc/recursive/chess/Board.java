package cc.recursive.chess;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class Board {
    // Board cells with piece when present, null when not.
    private Piece[][] cells = new Piece[Constants.Columns][Constants.Rows];
    // Castlings that may still be available.
    private List<Castling> castlings = new ArrayList<Castling>();
    // En-passant square when last move is pawn moving 2 squares from initial row.
    private Square enPassantSquare = null;
    // Number of half-moves since the last capture or pawn advance, used for the fifty-move draw.
    private int halfMoveClock = 0;
    // Number of half-moves since the start of the game.
    private int halfMoves = 0;
    // Whether crazyhouse rules are enabled.
    private boolean crazyHouse = false;
    // Reserve pieces for each color, used for crazyhouse.
    private int[][] reservePieces = new int[Constants.Colors][Constants.ReservePieces.length];
    // List of squares of highlight to show last move in viewer.
    private List<Square> highlightSquares = new ArrayList<Square>();

    public Board(Board other) {
        for (int column = 0; column < Constants.Columns; column++) {
            for (int row = 0; row < Constants.Rows; row++)
                cells[column][row] = other.cells[column][row];
        }
        for (Castling castling : other.castlings)
            castlings.add(new Castling(castling));
        enPassantSquare = other.enPassantSquare;
        halfMoveClock = other.halfMoveClock;
        halfMoves = other.halfMoves;
        crazyHouse = other.crazyHouse;
        for (int color = 0; color < Constants.Colors; color++) {
            for (int i = 0; i < Constants.ReservePieces.length; i++)
                reservePieces[color][i] = other.reservePieces[color][i];
        }
    }

    public Board(long seed, boolean crazyHouse) {
        this.crazyHouse = crazyHouse;

        if (seed == 0) {
            // seed=0 means classic chess initial row.
            PieceType[] initialRow = { PieceType.Rook, PieceType.Knight, PieceType.Bishop, PieceType.Queen, PieceType.King, PieceType.Bishop, PieceType.Knight, PieceType.Rook};
            int row = getInitialRow(Color.White);
            for (int column = 0; column < initialRow.length; column++)
                set(new Square(column, row), new Piece(initialRow[column], Color.White));
        } else {
            // Random picks on initial row for a total of 960 combinations.
            // Following procedure as described at https://en.wikipedia.org/wiki/Chess960#Setup
            int row = getInitialRow(Color.White);
            Random rng = new Random(seed);
            set(new Square(rng.nextInt(4) * 2, row), new Piece(PieceType.Bishop, Color.White));
            set(new Square(rng.nextInt(4) * 2 + 1, row), new Piece(PieceType.Bishop, Color.White));
            BiConsumer<Integer, PieceType> placePiece = (Integer emptyColumn, PieceType pieceType) -> {
                for (int column = 0; column < Constants.Columns; column++) {
                    Square square = new Square(column, row);
                    if (get(square) != null) continue;
                    if (emptyColumn == 0) {
                        set(square, new Piece(pieceType, Color.White));
                        return;
                    }
                    emptyColumn--;
                }
                throw new IllegalArgumentException("Not enough empty columns to satisfy argument");
            };
            placePiece.accept(rng.nextInt(6), PieceType.Queen);
            placePiece.accept(rng.nextInt(5), PieceType.Knight);
            placePiece.accept(rng.nextInt(4), PieceType.Knight);
            placePiece.accept(0, PieceType.Rook);
            placePiece.accept(0, PieceType.King);
            placePiece.accept(0, PieceType.Rook);
        }
        
        for (int column = 0; column < Constants.Columns; column++) {
            Piece piece = get(new Square(column, getInitialRow(Color.White)));
            // Copy initial row on black side.
            set(new Square(column, getInitialRow(Color.Black)), piece.invert());
            // Create pawns on second row of each side.
            set(new Square(column, getPawnRow(Color.White)), new Piece(PieceType.Pawn, Color.White));
            set(new Square(column, getPawnRow(Color.Black)), new Piece(PieceType.Pawn, Color.Black));
            // Keep track of castling columns.
            if (piece.getPieceType() == PieceType.Rook)
                castlings.add(new Castling(column, true));
        }
    }

    // Parse from FEN representation. https://en.wikipedia.org/wiki/Forsyth-Edwards_Notation
    public Board(String fen, boolean crazyHouse) throws ParseException {
        this.crazyHouse = crazyHouse;

        String[] parts = fen.split(" ");
        if (parts.length != 6) throw new ParseException("FEN string is malformed, needs 6 fields.", 0);
        {
            int column = 0, row = Constants.Rows - 1;
            for (char c : parts[0].toCharArray()) {
                if (c == '/') {
                    row--;
                    column = 0;
                }
                else if (c >= '1' && c <= '9')
                    column += c - '0';
                else {
                    if (row >= 0 && row < Constants.Rows && column >= 0 && column < Constants.Columns) {
                        if (crazyHouse && c == '~') {
                            column--;
                            Square square = new Square(column, row);
                            Piece piece = get(square);
                            if (piece == null) throw new ParseException("Found ~ symbol without an associated piece.", 0);
                            set(square, piece.promote());
                        } else
                            set(new Square(column, row), Piece.fromChar(c));
                    } else if (crazyHouse && row < 0) {
                        PieceType pieceType = PieceType.fromChar(c);
                        if (pieceType == PieceType.King) throw new ParseException("Found a king in a reserve.", 0);
                        addReserve(Character.isUpperCase(c) ? Color.White : Color.Black, pieceType, 1);
                    }
                    column++;
                }
            }
        }
        Color colorToPlay = Color.fromChar(parts[1].charAt(0));
        if (!parts[2].equals("-")) {
            for (char c : parts[2].toCharArray()) {
                int column = Square.charToColumn(c);
                Castling castling = null;
                for (Castling iter : castlings) {
                    if (iter.getColumn() != column) continue;
                    castling = iter;
                    break;
                }
                if (castling == null) {
                    if (castlings.size() >= Constants.Castlings) throw new ParseException("Too many castlings found.", 0);
                    castling = new Castling(column, false);
                    castlings.add(castling);
                }
                castling.setAllowed(Character.isUpperCase(c) ? Color.White : Color.Black, true);
            }
        }
        if (!parts[3].equals("-"))
            enPassantSquare = new Square(parts[3]);
        halfMoveClock = Integer.parseInt(parts[4]);
        if (halfMoveClock < 0) throw new ParseException("Half-move clock must be greater or equal to zero.", 0);
        halfMoves = (Integer.parseInt(parts[5]) - 1) * 2 + (colorToPlay == Color.White ? 0 : 1);
        if (halfMoves < 0) throw new ParseException("Fullmove number must be greater than zero.", 0);
    }

    public Piece get(Square square) { return cells[square.getColumn()][square.getRow()]; }
    private void set(Square square, Piece piece) { cells[square.getColumn()][square.getRow()] = piece; }

    public Color getColorToPlay() { return halfMoves % 2 == 0 ? Color.White : Color.Black; }
    public int getInitialRow(Color color) { return color == Color.White ? 0 : Constants.Rows - 1; }
    public int getPawnRow(Color color) { return color == Color.White ? 1 : Constants.Rows - 2; }
    public int getPawnFront(Color color) { return color == Color.White ? 1 : -1; }
    public List<Square> getHighlightSquares() { return highlightSquares; }

    public int getHalfMoves() { return halfMoves; }
    public int getHalfMoveClock() { return halfMoveClock; }

    public int getReserve(Color color, PieceType pieceType) {
        if (pieceType == PieceType.King) throw new IllegalArgumentException("Reserve pieces does not include kings.");
        return reservePieces[color.ordinal()][pieceType.ordinal()];
    }
    public void addReserve(Color color, PieceType pieceType, int count) { 
        if (pieceType == PieceType.King) throw new IllegalArgumentException("Reserve pieces does not include kings.");
        reservePieces[color.ordinal()][pieceType.ordinal()] += count;
        if (reservePieces[color.ordinal()][pieceType.ordinal()] < 0) throw new IllegalArgumentException("Reserve count has become negative.");
    }

    // Encode board to FEN representation. https://en.wikipedia.org/wiki/Forsyth-Edwards_Notation
    public String toFenString(boolean forRepetition) {
        StringBuilder sb = new StringBuilder();
        for (int row = Constants.Rows - 1; row >= 0; row--) {
            int empty = 0;
            for (int column = 0; column < Constants.Columns; column++) {
                Square square = new Square(column, row);
                Piece piece = get(square);
                if (piece == null)
                    empty++;
                else {
                    if (empty > 0) {
                        sb.append((char)('0' + empty));
                        empty = 0;
                    }
                    sb.append(piece.toChar());
                    // Follow lichess crazyhouse convention and add symbol to indicate promoted pawn.
                    if (crazyHouse && piece.isPromoted())
                        sb.append('~');
                }
            }
            // Copy-paste because Java lambdas are exceptionally weak.
            if (empty > 0) {
                sb.append((char)('0' + empty));
                empty = 0;
            }
            if (row > 0)
                sb.append('/');
        }
        // Follow lichess crazyhouse convention and encode reserve as an extra row.
        if (crazyHouse) {
            sb.append('/');
            for (Color color : Color.values()) {
                for (PieceType pieceType : Constants.ReservePieces) {
                    int count = getReserve(color, pieceType);
                    if (count == 0) continue;
                    for (int j = 0; j < count; j++)
                        sb.append(pieceType.toChar(color));
                }
            }
        }
        sb.append(' ');
        sb.append(getColorToPlay().toChar());
        sb.append(' ');
        int beforeCastling = sb.length();
        for (Color color : Color.values()) {
            for (Castling castling : castlings) {
                char c = Square.columnToChar(castling.getColumn());
                if (castling.isAllowed(color)) sb.append(color == Color.White ? Character.toUpperCase(c) : c);
            }
        }
        if (sb.length() == beforeCastling) sb.append('-');
        sb.append(' ');
        if (!forRepetition || enPassantSquare == null)
            sb.append(enPassantSquare != null ? enPassantSquare.toString() : '-');
        else {
            // For repetition check, only print en-passant square if such a capture was possible.
            boolean printSquare = false;
            int front = getPawnFront(getColorToPlay());
            for (int column = -1; column <= 1; column += 2) {
                Square square = enPassantSquare.offset(column, -front);
                if (square == null) continue;
                Piece piece = get(square);
                if (piece == null) continue;
                if (piece.getPieceType() == PieceType.Pawn && piece.getColor() == getColorToPlay()) {
                    printSquare = true;
                    break;
                }
            }
            sb.append(printSquare ? enPassantSquare.toString() : '-');
        }
        // These fields do not matter for repetition check.
        if (!forRepetition) {
            sb.append(' ');
            sb.append(halfMoveClock);
            sb.append(' ');
            sb.append(1 + halfMoves / 2);
        }
        return sb.toString();
    }

    // Computes whether either side has enough material to deliver a checkmate in theory. https://en.wikipedia.org/wiki/Draw_(chess)#Draws_in_all_games
    public boolean hasSufficientMaterial() {
        // Can always drop pieces in crazyhouse.
        if (crazyHouse) return true;
        
        Piece lastPiece = null;
        Square lastSquare = null;
        for (int column = 0; column < Constants.Columns; column++) {
            for (int row = 0; row < Constants.Rows; row++) {
                Square square = new Square(column, row);
                Piece piece = get(square);
                if (piece == null) continue;
                PieceType pieceType = piece.getPieceType();
                if (pieceType == PieceType.King) continue;
                // Those pieces are always enough.
                if (pieceType == PieceType.Pawn || pieceType == PieceType.Rook || pieceType == PieceType.Queen)
                    return true;
                if (lastPiece == null) {
                    lastPiece = piece;
                    lastSquare = square;
                    continue;
                }
                // Each side with one bishop on the same color is not enough.
                if (lastPiece.getColor() != piece.getColor() && lastPiece.getPieceType() == PieceType.Bishop && piece.getPieceType() == PieceType.Bishop && lastSquare.isLight() == square.isLight())
                    continue;
                // Two pieces on board is otherwise enough.
                return true;
            }
        }

        // One or zero minor piece with no other piece is not enough to checkmate.
        return false;
    }

    private void addRangeMoves(List<Move> moves, Square from, int limit, int deltaColumn, int deltaRow) {
        Color selfColor = getColorToPlay();
        Square to = from;
        for (int range = 0; range < limit; range++) {
            to = to.offset(deltaColumn, deltaRow);
            if (to == null) return;
            Piece toPiece = get(to);
            if (toPiece == null || toPiece.getColor() != selfColor)
                moves.add(new Move(from, to));
            if (toPiece != null) return;
        }
    }

    private void addDiagonalMoves(List<Move> moves, Square from, int limit) {
        addRangeMoves(moves, from, limit, -1, -1);
        addRangeMoves(moves, from, limit, -1,  1);
        addRangeMoves(moves, from, limit,  1, -1);
        addRangeMoves(moves, from, limit,  1,  1);
    }

    private void addStraightMoves(List<Move> moves, Square from, int limit) {
        addRangeMoves(moves, from, limit,  0, -1);
        addRangeMoves(moves, from, limit,  0,  1);
        addRangeMoves(moves, from, limit, -1,  0);
        addRangeMoves(moves, from, limit,  1,  0);
    }

    // Check if a square can be attacked by an opponent piece.
    private boolean isSquareAttacked(Square to, Color selfColor) {
        // Check range in all directions.
        for (int deltaColumn = -1; deltaColumn <= 1; deltaColumn++) {
            for (int deltaRow = -1; deltaRow <= 1; deltaRow++) {
                if (deltaRow == 0 && deltaColumn == 0) continue;
                boolean isDiagonal = Math.abs(deltaRow) == Math.abs(deltaColumn);
                Square from = to;
                for (int range = 0; range < Constants.RangeLimit; range++) {
                    from = from.offset(deltaColumn, deltaRow);
                    if (from == null) break;
                    Piece fromPiece = get(from);
                    if (fromPiece == null) continue;
                    if (fromPiece.getColor() == selfColor) break;

                    PieceType pieceType = fromPiece.getPieceType();
                    if (pieceType == PieceType.Pawn && isDiagonal && to.getRow() - from.getRow() == getPawnFront(selfColor.opposite()))
                        return true;
                    if (pieceType == PieceType.Queen || (pieceType == PieceType.King && range == 0))
                        return true;
                    if (pieceType == (isDiagonal ? PieceType.Bishop : PieceType.Rook))
                        return true;
                    break;
                }
            }
        }

        // Check knights.
        for (int i = 0; i < Constants.KnightMoves; i++) {
            Square from = to.offset(Constants.KnightDeltaColumn[i], Constants.KnightDeltaRow[i]);
            if (from == null) continue;
            Piece fromPiece = get(from);
            if (fromPiece != null && fromPiece.getColor() != selfColor && fromPiece.getPieceType() == PieceType.Knight)
                return true;
        }

        return false;
    }

    // Find the square where the king of given color is located. Cannot be null.
    private Square findKingSquare(Color color) {
        for (int column = 0; column < Constants.Columns; column++) {
            for (int row = 0; row < Constants.Rows; row++) {
                Square square = new Square(column, row);
                Piece piece = get(square);
                if (piece != null && piece.getColor() == color && piece.getPieceType() == PieceType.King)
                    return square;
            }
        }
        throw new IllegalStateException("Unable to find own king");
    }

    // Returns whether the king of given color is under check.
    public boolean isKingAttacked(Color color) {
        return isSquareAttacked(findKingSquare(color), color);
    }

    // Generate all legal moves from current position.
    public List<Move> generateMoves() {
        List<Move> moves = new ArrayList<Move>();
        Color selfColor = getColorToPlay();
        Color opponentColor = selfColor.opposite();

        for (int fromColumn = 0; fromColumn < Constants.Columns; fromColumn++) {
            for (int fromRow = 0; fromRow < Constants.Rows; fromRow++) {
                Square from = new Square(fromColumn, fromRow);
                Piece piece = get(from);

                // Drop reserve pieces on empty squares.
                if (crazyHouse && piece == null) {
                    for (PieceType pieceType : Constants.ReservePieces) {
                        if (getReserve(selfColor, pieceType) == 0) continue;
                        // Cannot drop pawns on initial rows.
                        if (pieceType == PieceType.Pawn && (fromRow == 0 || fromRow == Constants.Rows - 1)) continue;
                        moves.add(new Move(null, from, pieceType));
                    }
                }

                if (piece == null || piece.getColor() != selfColor) continue;
                PieceType pieceType = piece.getPieceType();

                if (pieceType == PieceType.Pawn) {
                    // Handle promotion cases when pawn ends on opponent's initial row.
                    BiConsumer<Square, Square> addPromotionMoves = (Square f, Square t) -> {
                        if (t.getRow() != getInitialRow(opponentColor))
                            moves.add(new Move(f, t));
                        else {
                            moves.add(new Move(f, t, PieceType.Knight));
                            moves.add(new Move(f, t, PieceType.Bishop));
                            moves.add(new Move(f, t, PieceType.Rook));
                            moves.add(new Move(f, t, PieceType.Queen));
                        }
                    };
                    // Move pawn ahead.
                    int front = getPawnFront(selfColor);
                    Square to = from.offset(0, front);
                    if (to != null && get(to) == null) {
                        addPromotionMoves.accept(from, to);
                        // Can move 2 squares from pawn row.
                        if (from.getRow() == getPawnRow(selfColor)) {
                            Square to2 = from.offset(0, front*2);
                            if (to2 != null && get(to2) == null)
                                moves.add(new Move(from, to2));
                        }
                    }
                    // Pawn capture on diagonals including en-passant.
                    for (int deltaColumn = -1; deltaColumn <= 1; deltaColumn += 2) {
                        to = from.offset(deltaColumn, front);
                        if (to == null) continue;
                        Piece toPiece = get(to);
                        if ((toPiece != null && toPiece.getColor() == opponentColor) || to.equals(enPassantSquare))
                            addPromotionMoves.accept(from, to);
                    }
                } else if (pieceType == PieceType.Knight) {
                    // Check all L shapes.
                    for (int i = 0; i < Constants.KnightMoves; i++) {
                        Square to = from.offset(Constants.KnightDeltaColumn[i], Constants.KnightDeltaRow[i]);
                        if (to == null) continue;
                        Piece toPiece = get(to);
                        if (toPiece != null && toPiece.getColor() == selfColor) continue;
                        moves.add(new Move(from, to));
                    }
                } else if (pieceType == PieceType.Bishop)
                    addDiagonalMoves(moves, from, Constants.RangeLimit);
                else if (pieceType == PieceType.Rook)
                    addStraightMoves(moves, from, Constants.RangeLimit);
                else if (pieceType == PieceType.Queen) {
                    addDiagonalMoves(moves, from, Constants.RangeLimit);
                    addStraightMoves(moves, from, Constants.RangeLimit);
                } else if (pieceType == PieceType.King) {
                    addDiagonalMoves(moves, from, 1);
                    addStraightMoves(moves, from, 1);
                    // Check for castling move.
                    for (Castling castling : castlings) {
                        if (!castling.isAllowed(selfColor)) continue;
                        int rookFromColumn = castling.getColumn();
                        int kingFromColumn = fromColumn;
                        boolean queenCastling = rookFromColumn < kingFromColumn;
                        int deltaColumn = queenCastling ? -1 : 1;
                        int rookToColumn = queenCastling ? Constants.QueenCastlingRookColumn : Constants.KingCastlingRookColumn;
                        int kingToColumn = queenCastling ? Constants.QueenCastlingKingColumn : Constants.KingCastlingKingColumn;

                        boolean valid = true;
                        // The final squares must not be occupied by other pieces.
                        valid &= rookToColumn == kingFromColumn || rookToColumn == rookFromColumn || get(new Square(rookToColumn, fromRow)) == null;
                        valid &= kingToColumn == kingFromColumn || kingToColumn == rookFromColumn || get(new Square(kingToColumn, fromRow)) == null;
                        // There must be no piece obstructing between king and rook.
                        for (int column = kingFromColumn + deltaColumn; valid && column != rookFromColumn; column += deltaColumn)
                            valid &= get(new Square(column, fromRow)) == null;
                        // There must be no enemy piece attacking the squares between king's initial and final position.
                        for (int column = kingFromColumn; valid && column != kingToColumn + deltaColumn; column += deltaColumn)
                            valid &= !isSquareAttacked(new Square(column, fromRow), selfColor);
                        if (valid)
                            moves.add(new Move(from, new Square(rookFromColumn, fromRow)));
                    }
                }
            }
        }

        // Remove moves that would leave the king under check.
        moves.removeIf(move -> {
            Board nextBoard = new Board(this);
            nextBoard.applyMoveUnsafe(move);
            return nextBoard.isKingAttacked(selfColor);
        });

        return moves;
    }

    public boolean isMoveCastling(Move move) {
        if (move.getFrom() == null)
            return false;
        Piece fromPiece = get(move.getFrom());
        Piece toPiece = get(move.getTo());
        return fromPiece.getPieceType() == PieceType.King && toPiece != null && toPiece.getPieceType() == PieceType.Rook && toPiece.getColor() == fromPiece.getColor();
    }

    // Simply performs a move assuming it is **valid** and **legal**!
    public void applyMoveUnsafe(Move move) {
        highlightSquares.clear();

        Square from = move.getFrom(), to = move.getTo();
        Piece fromPiece = from != null ? get(from) : null, toPiece = get(to);
        Color selfColor = getColorToPlay();
        Color opponentColor = selfColor.opposite();

        if (from == null) {
            if (!crazyHouse) throw new IllegalArgumentException("Attempted to apply a drop piece move in a non-crazyhouse game.");
            PieceType pieceType = move.getPieceType().get();
            set(move.getTo(), new Piece(pieceType, selfColor));
            addReserve(selfColor, pieceType, -1);
        } else {
            if (fromPiece != null) {
                // When king moves, invalidate its castlings.
                if (fromPiece.getPieceType() == PieceType.King) {
                    for (Castling castling : castlings)
                        castling.setAllowed(selfColor, false);
                }

                // If rook moved from initial position, invalidate its castling.
                if (fromPiece.getPieceType() == PieceType.Rook && from.getRow() == getInitialRow(selfColor)) {
                    for (Castling castling : castlings) {
                        if (castling.getColumn() != from.getColumn()) continue;
                        castling.setAllowed(selfColor, false);
                    }
                }

                // Pawn moving to en-passant square implies it captured en-passant.
                if (enPassantSquare != null && fromPiece.getPieceType() == PieceType.Pawn && to.equals(enPassantSquare))
                    set(new Square(to.getColumn(), from.getRow()), null);

                // Update en-passant square if pawn moved 2 rows.
                enPassantSquare = null;
                if (fromPiece.getPieceType() == PieceType.Pawn && Math.abs(from.getRow() - to.getRow()) == 2)
                    enPassantSquare = new Square(from.getColumn(), (from.getRow() + to.getRow()) / 2);
            }

            // A captured rook on its initial row invalidates its castling.
            if (toPiece != null && toPiece.getPieceType() == PieceType.Rook && toPiece.getColor() == opponentColor && to.getRow() == getInitialRow(opponentColor)) {
                for (Castling castling : castlings) {
                    if (castling.getColumn() != to.getColumn()) continue;
                    castling.setAllowed(opponentColor, false);
                }
            }

            // Update the board.
            if (isMoveCastling(move)) {
                set(from, null);
                set(to, null);
                int row = getInitialRow(selfColor);
                boolean queenCastling = to.getColumn() < from.getColumn();
                Square rookSquare = new Square(queenCastling ? Constants.QueenCastlingRookColumn : Constants.KingCastlingRookColumn, row);
                Square kingSquare = new Square(queenCastling ? Constants.QueenCastlingKingColumn : Constants.KingCastlingKingColumn, row);
                set(rookSquare, toPiece);
                set(kingSquare, fromPiece);
                highlightSquares.add(rookSquare);
                highlightSquares.add(kingSquare);
            } else {
                // Add captured piece to reserve in crazyhouse.
                if (crazyHouse && toPiece != null) {
                    PieceType pieceType = toPiece.getPieceType();
                    if (toPiece.isPromoted())
                        pieceType = PieceType.Pawn;
                    addReserve(selfColor, pieceType, 1);
                }

                if (move.getPieceType().isPresent())
                    set(move.getTo(), new Piece(move.getPieceType().get(), selfColor, true));
                else
                    set(move.getTo(), get(move.getFrom()));
                set(move.getFrom(), null);
            }
        }

        if (from != null) highlightSquares.add(from);
        highlightSquares.add(to);

        // Update clocks.
        // Halfmove clock is reset when a pawn moves or a capture occurs.
        if ((fromPiece != null && fromPiece.getPieceType() == PieceType.Pawn) || (toPiece != null && toPiece.getColor() != selfColor))
            halfMoveClock = 0;
        else
            halfMoveClock++;
        halfMoves++;
    }

    // To be called on the position before the move is made, assuming it is **valid** and **legal**!
    // https://en.wikipedia.org/wiki/Algebraic_notation_(chess)
    public String getAlgebraicMoveUnsafe(Move move) {
        StringBuilder sb = new StringBuilder();
        if (move.getFrom() == null) {
            sb.append(move.getPieceType().get().toChar());
            sb.append('@');
            sb.append(move.getTo().toString());
        } else if (isMoveCastling(move))
            sb.append(move.getTo().getColumn() < move.getFrom().getColumn() ? "O-O-O" : "O-O");
        else {
            Piece fromPiece = get(move.getFrom());
            Piece toPiece = get(move.getTo());
            boolean takes = toPiece != null || (fromPiece.getPieceType() == PieceType.Pawn && move.getFrom().getColumn() != move.getTo().getColumn());

            if (fromPiece.getPieceType() != PieceType.Pawn) {
                sb.append(fromPiece.getPieceType().toChar());
                // Check for disambiguation.
                List<Move> moves = generateMoves();
                moves.removeIf(x -> x.getFrom() == null || !x.getTo().equals(move.getTo()) || get(x.getFrom()).getPieceType() != fromPiece.getPieceType());
                if (moves.size() > 1) {
                    moves.removeIf(x -> x.getFrom().getColumn() != move.getFrom().getColumn());
                    if (moves.size() == 1)
                        sb.append(Square.columnToChar(move.getFrom().getColumn()));
                    else {
                        moves.removeIf(x -> x.getFrom().getRow() != move.getFrom().getRow());
                        if (moves.size() == 1)
                            sb.append(Square.rowToChar(move.getFrom().getRow()));
                        else {
                            sb.append(Square.columnToChar(move.getFrom().getColumn()));
                            sb.append(Square.rowToChar(move.getFrom().getRow()));
                        }
                    }
                }
            } else if (takes)
                sb.append(Square.columnToChar(move.getFrom().getColumn()));
            if (takes)
                sb.append('x');
            sb.append(move.getTo().toString());

            if (move.getPieceType().isPresent()) {
                sb.append('=');
                sb.append(move.getPieceType().get().toChar());
            }
        }

        Board nextBoard = new Board(this);
        nextBoard.applyMoveUnsafe(move);
        if (nextBoard.isKingAttacked(nextBoard.getColorToPlay())) {
            if (nextBoard.generateMoves().size() == 0)
                sb.append('#'); // Checkmate
            else
                sb.append('+'); // Check
        }

        return sb.toString();
    }
}
