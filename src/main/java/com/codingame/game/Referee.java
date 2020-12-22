package com.codingame.game;

import java.text.ParseException;
import java.util.List;
import java.util.Random;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.google.inject.Inject;

import cc.recursive.chess.Board;
import cc.recursive.chess.Color;
import cc.recursive.chess.Constants;
import cc.recursive.chess.Game;
import cc.recursive.chess.GameException;
import cc.recursive.chess.GameResult;
import cc.recursive.chess.Move;
import cc.recursive.chess.ViewData;
import cc.recursive.chess.ViewGlobalData;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    // Game currently played.
    private Game game = null;
    // Last move played.
    private Move lastMove = null;
    // Whether the last player has offered a draw.
    private boolean drawOffered = false;
    // Number of completed games so far.
    private int gameCount = 0;
    // Random number generator used for the 'random' command.
    private Random rng = new Random();
    // Whether crazyhouse rules are enabled.
    private boolean crazyHouse = false;

    @Override
    public void init() {
        // Max turns don't actually matter, the total time does.
        gameManager.setMaxTurns(Integer.MAX_VALUE);

        crazyHouse = gameManager.getLeagueLevel() == 2;

        ViewGlobalData viewGlobalData = new ViewGlobalData();
        viewGlobalData.setCrazyHouse(crazyHouse);
        gameManager.setViewGlobalData("chess", viewGlobalData);

        newGame();
    }

    @Override
    public void gameTurn(int turn) {
        // First turn is held outside the game and used for configuration purposes.
        if (turn <= 2) {
            Player player = gameManager.getPlayer(turn - 1);
            player.sendInputLine("2");
            player.sendInputLine(String.format("crazyHouse %d", crazyHouse ? 1 : 0));
            player.sendInputLine(String.format("maxMoves %d", Constants.MaxMovesPerGame));
            player.execute();
            try {
                String inputConfiguration = player.getOutputs().get(0);
                player.setInputVariables(inputConfiguration);

            } catch (TimeoutException e) {
                handlePlayerError(player, "Timeout");
            }
            setViewData(game.getViewData());
            return;
        }

        // Empty frame for setting up a new game.
        if (game.getGameResult() != GameResult.Undecided) {
            gameCount++;
            newGame();
            return;
        }

        Board board = game.getBoard();
        Player currPlayer = getPlayerFromColor(board.getColorToPlay());

        // Produce input according to player's configuration.
        for (String variable : currPlayer.getInputVariables()) {
            if (variable.equals("fen"))
                currPlayer.sendInputLine(board.toFenString(false));
            else if (variable.equals("moves")) {
                List<Move> moves = board.generateMoves();
                currPlayer.sendInputLine(Integer.toString(moves.size()));
                for (Move move : moves)
                    currPlayer.sendInputLine(move.toString());
            } else if (variable.equals("lastmove"))
                currPlayer.sendInputLine(lastMove != null ? lastMove.toString() : "none");
            else if (variable.equals("draw"))
                currPlayer.sendInputLine(drawOffered ? "1" : "0");
            else if (variable.equals("game"))
                currPlayer.sendInputLine(Integer.toString(gameCount + 1));
            else if (variable.equals("score"))
                currPlayer.sendInputLine(String.format("%d %d", currPlayer.getScore(), getPlayerFromColor(board.getColorToPlay().opposite()).getScore()));
        }
        currPlayer.execute();

        ViewData viewData = null;
        String status = null;
        String comment = null;
        try {
            String output = currPlayer.getOutputs().get(0);

            // Split comment from output.
            int spaceIndex = output.indexOf(' ');
            if (spaceIndex != -1) {
                comment = output.substring(spaceIndex + 1);
                if (comment.length() > Constants.MaxCommentLength)
                    comment = comment.substring(0, Constants.MaxCommentLength);
                output = output.substring(0, spaceIndex);
            }

            if (output.length() == 0)
                throw new ParseException("Player output is missing!", 0);

            // Check if a draw offer was accepted.
            if (drawOffered && output.equals("draw")) {
                game.setGameResult(GameResult.DrawByAgreement);
                viewData = game.getViewData();
            // Check if the game was resigned.
            } else if (output.equals("resign")) {
                game.setGameResult(board.getColorToPlay() == Color.White ? GameResult.WhiteResigns : GameResult.BlackResigns);
                viewData = game.getViewData();
            } else {
                drawOffered = false;
                // Check if a draw has been offered.
                if (output.charAt(output.length() - 1) == '=') {
                    drawOffered = true;
                    output = output.substring(0, output.length() - 1);
                }
                Move move;
                if (output.equals("random")) {
                    List<Move> moves = board.generateMoves();
                    move = moves.get(rng.nextInt(moves.size()));
                }
                else
                    move = new Move(output);
                lastMove = move;
                viewData = game.applyMove(move);
            }

            GameResult result = game.getGameResult();
            // Handle endgame result.
            if (result != GameResult.Undecided) {
                if (result == GameResult.WhiteWins || result == GameResult.BlackWins) {
                    Player winner = getPlayerFromColor(result == GameResult.WhiteWins ? Color.White : Color.Black);
                    winner.setScore(winner.getScore() + 2);
                    gameManager.addTooltip(winner, winner.getNicknameToken() + " has checkmated the opponent!");
                    viewData.setResult(result == GameResult.WhiteWins ? "1-0" : "0-1");
                    status = "Checkmate.";
                } else if (result == GameResult.WhiteResigns || result == GameResult.BlackResigns) {
                    Player winner = getPlayerFromColor(result == GameResult.WhiteResigns ? Color.Black : Color.White);
                    winner.setScore(winner.getScore() + 2);
                    gameManager.addTooltip(currPlayer, currPlayer.getNicknameToken() + " has resigned!");
                    viewData.setResult(result == GameResult.WhiteResigns ? "0-1" : "1-0");
                    status = String.format("%s resigned.", board.getColorToPlay());
                } else {
                    for (int i = 0; i < 2; i++) {
                        Player player = gameManager.getPlayer(i);
                        player.setScore(player.getScore() + 1);
                    }
                    viewData.setResult("1/2-1/2");
                    if (result == GameResult.Stalemate) {
                        gameManager.addTooltip(currPlayer, currPlayer.getNicknameToken() + " has caused a stalemate!");
                        status = "Stalemate.";
                    }
                    else if (result == GameResult.Repetition) {
                        gameManager.addTooltip(currPlayer, currPlayer.getNicknameToken() + " has repeated the position three times!");
                        status = "Draw by repetition.";
                    }
                    else if (result == GameResult.InsufficientMaterial) {
                        gameManager.addTooltip(currPlayer, "Neither player has sufficient material to win!");
                        status = "Draw by insufficient material.";
                    }
                    else if (result == GameResult.FiftyMove) {
                        gameManager.addTooltip(currPlayer, "Draw by the fifty-move rule!");
                        status = "Draw by the fifty-move rule.";
                    }
                    else if (result == GameResult.ForcedDraw) {
                        gameManager.addTooltip(currPlayer, String.format("Forced draw after %d moves!", Constants.MaxMovesPerGame));
                        status = "Forced draw.";
                    }
                    else if (result == GameResult.DrawByAgreement) {
                        gameManager.addTooltip(currPlayer, "Draw by agreement!");
                        status = "Draw by agreement.";
                    }
                }

                if (gameCount == 1)
                    gameManager.endGame();
            } else {
                if (drawOffered)
                    status = "A draw was offered.";
            }
        } catch (ParseException e) {
            handlePlayerError(currPlayer, e.getMessage());
        } catch (GameException e) {
            handlePlayerError(currPlayer, e.getMessage());
        } catch (TimeoutException e) {
            handlePlayerError(currPlayer, "Timeout");
        }

        if (viewData == null)
            viewData = game.getViewData();
        viewData.setStatus(status);
        viewData.setComment(comment);
        setViewData(viewData);
    }

    private Player getPlayerFromColor(Color color) {
        return gameManager.getPlayer(color == Color.White ? gameCount % 2 : 1 - gameCount % 2);
    }

    private void newGame() {
        game = new Game(gameManager.getSeed(), crazyHouse);
        setViewData(game.getViewData());
        lastMove = null;
        drawOffered = false;
    }

    private void handlePlayerError(Player player, String message) {
        gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " eliminated: " + message));
        player.deactivate(message);
        player.setScore(-1);
        gameManager.endGame();
    }

    private void setViewData(ViewData viewData) {
        viewData.setGame(gameCount);
        viewData.setScores(gameManager.getPlayer(0).getScore(), gameManager.getPlayer(1).getScore());
        gameManager.setViewData("chess", viewData);
    }
}
