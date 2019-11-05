package sc.player2020.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.framework.plugins.Player;
import sc.player2020.Starter;
import sc.plugin2020.DragMove;
import sc.plugin2020.GameState;
import sc.plugin2020.IGameHandler;
import sc.plugin2020.Field;
import sc.plugin2020.Move;
import sc.plugin2020.SetMove;
import sc.plugin2020.Piece;
import sc.plugin2020.PieceType;
import sc.plugin2020.util.CubeCoordinates;
import sc.plugin2020.util.GameRuleLogic;
import sc.plugin2020.util.Constants;
import sc.shared.GameResult;
import sc.shared.InvalidGameStateException;
import sc.shared.InvalidMoveException;
import sc.shared.PlayerColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Das Herz des Clients:
 * Eine sehr simple Logik, die ihre Zuege zufaellig waehlt,
 * aber gueltige Zuege macht.
 * Ausserdem werden zum Spielverlauf Konsolenausgaben gemacht.
 */
public class BeatMeBot implements IGameHandler {
  private static final Logger log = LoggerFactory.getLogger(BeatMeBot.class);

  private Starter client;
  private GameState gameState;
  private Player currentPlayer;
  
  private int aufrufe;
  private Move bestMove;
  private String[] moveList = new String[Consts.ALPHABETA_DEPTH];

  /**
   * Erzeugt ein neues Strategieobjekt, das zufaellige Zuege taetigt.
   *
   * @param client Der zugrundeliegende Client, der mit dem Spielserver kommuniziert.
   */
  public BeatMeBot(Starter client) {
    this.client = client;
  }

  /**
   * {@inheritDoc}
   */
  public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
    //log.info("Das Spiel ist beendet.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRequestAction() {
    
    final long timeStart = System.currentTimeMillis();
    
    int turn = gameState.getTurn();

    Lib.pln("", Consts.PRINT_NEW_ROUND);
    Lib.pln("* * * * * * * * * * * *", Consts.PRINT_NEW_ROUND);
    Lib.pln("* N e u e   R u n d e *", Consts.PRINT_NEW_ROUND);
    Lib.pln("* * * * * * * * * * * *", Consts.PRINT_NEW_ROUND);
    Lib.pln("", Consts.PRINT_NEW_ROUND);
    Lib.pln("  Turn: " + turn, Consts.PRINT_ROUND_INFO);
    
    // TURN 0 and 1
    if (turn < 2) {
    	CubeCoordinates beeMove = Lib.findMove(gameState, turn);
    	Piece bee = new Piece(gameState.getCurrentPlayerColor(), PieceType.BEE);
    	bestMove = new SetMove(bee, beeMove);
    	
    // ALPHA BETA
    } else {
    	
    	startAlphaBeta();
	    
    }
    
    // Board ausgeben
    if (Consts.PRINT_BOARD) {
    	Lib.printBoard(gameState.getBoard());
    }
    
    Lib.pln("  BestMove: ", Consts.PRINT_MOVE);
	sendAction(bestMove);
	
	final long timeEnd = System.currentTimeMillis();
	Lib.pln("  Lauftzeit: " + (timeEnd - timeStart) + "ms.", Consts.PRINT_TIME);
	
  }
  
	private void startAlphaBeta() {
		aufrufe = 0;

		boolean error = false;

		try {
			// Eigentlicher Aufruf der Alphabeta
			alphaBeta(Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Consts.ALPHABETA_DEPTH);
		} catch (InvalidGameStateException e) {
			error = true;
			e.printStackTrace();
		} catch (InvalidMoveException e) {
			error = true;
			e.printStackTrace();
		}
		// ERROR
		if (error) {
			if (bestMove.equals(null) == true) {
				List<Move> possibleMoves = GameRuleLogic.getPossibleMoves(gameState);
				bestMove = possibleMoves.get((int) (Math.random() * possibleMoves.size()));
				Lib.pln("RND MOVE: " + bestMove.toString(), Consts.PRINT_ALPHABETA);
			}
		}
	}

	private int alphaBeta(int alpha, int beta, int tiefe) throws InvalidGameStateException, InvalidMoveException {
		++aufrufe;
		// Abbruchkriterium
		if ((tiefe == 0) || endOfGame()) {
			int value = rateAlphaBeta();
			if (Consts.SHOW_HEADER) {
				Lib.pln("", true);
				Lib.pln("***N*E*W***M*O*V*E***", true);
				Lib.pln("Value: " + value + " - Tiefe: " + Consts.ALPHABETA_DEPTH + " - Aufrufe: " + aufrufe + " - Turn: "
						+ gameState.getTurn() + " - Round: " + gameState.getRound(), true);
			}
			if (Consts.SHOW_MOVES) {
				for (String moveStr : moveList) {
					Lib.pln(moveStr, true);
				}
			}
			return value;
		}
		boolean PVgefunden = false;
		int best = Integer.MIN_VALUE + 1;
		List<Move> moves = GameRuleLogic.getPossibleMoves(gameState);

		for (Move move : moves) {
			moveList[Consts.ALPHABETA_DEPTH - tiefe] = move.toString(); //+ " "
					//+ this.gameState.getBoard().getField(move.x, move.y).getState().toString();
			GameState g = this.gameState.clone();
			GameRuleLogic.performMove(this.gameState, move);
			int wert;
			if (PVgefunden) {
				wert = -alphaBeta(-alpha - 1, -alpha, tiefe - 1);
				if (wert > alpha && wert < beta)
					wert = -alphaBeta(-beta, -wert, tiefe - 1);
			} else
				wert = -alphaBeta(-beta, -alpha, tiefe - 1);
			this.gameState = g;
			if (wert > best) {
				if (wert >= beta)
					return wert;
				best = wert;
				if (tiefe == Consts.ALPHABETA_DEPTH) {
					// ZUG KOPIEREN? GEHT DAS SO?
					if (move.toString().substring(0, 1).equals("S")) { // SetMove
						SetMove setMove = (SetMove) move;
						bestMove = new SetMove(setMove.getPiece(), move.getDestination());
					} else {
						DragMove dragMove = (DragMove) move;
						bestMove = new DragMove(dragMove.getStart(), move.getDestination());
					}
					Lib.pln("NEW BEST MOVE: " + bestMove.toString() + " Value: " + best, Consts.PRINT_ALPHABETA);
				}
				if (wert > alpha) {
					alpha = wert;
					PVgefunden = true;
				}
			}
		}
		return best;
	}

	private int rateAlphaBeta() {
		int value = 0;

		// Show Board
		if (Consts.SHOW_BOARD) {
			Lib.printBoard(this.gameState.getBoard());
		}

		PlayerColor current;
		PlayerColor opponent;
		if (Consts.ALPHABETA_DEPTH % 2 == 0) {
			current = this.gameState.getCurrentPlayer().getColor();
		} else {
			current = this.gameState.getOtherPlayer().getColor();
		}
		opponent = current.opponent();

		// Alle Zeilen durchlaufen...
		for (int row = 0; row < 11; row++) {
			// Alle Spalten durchrattern...
			for (int col = 0; col < 11; col++) {
				// x, y, z berechnen
				int x = (int) (col - 2 - Math.round((row + 1) / 2)); 
				int z = row - 5;
				int y = -1 * (x + z);
				// Nur suchen, wenn Field auf Feld liegt
				if ((x >= -5) && (x <= 5) && (y >= -5) && (y <= 5)) {
					Field field = this.gameState.getBoard().getField(x, y, z);
					// Eigene Insekten
					if (field.getOwner().toString() == current.toString()) {
						if (field.getPieces().get(0).getType() == PieceType.ANT) {
							value++;
						}
					}
					// Gegnerische Mückenplage
					
				} // possible Field
			} // of for row
		} // of for col

		return value;
	}

	private boolean endOfGame() {
		// TODO Es muss noch abgefragt werden, ob ein Spieler gewonnen hat (max.
		// Schwarmgroesse)
		return (this.gameState.getRound() == Constants.ROUND_LIMIT);
	}

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUpdate(Player player, Player otherPlayer) {
    currentPlayer = player;
    // log.info("Spielerwechsel: " + player.getColor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUpdate(GameState gameState) {
    this.gameState = gameState;
    currentPlayer = gameState.getCurrentPlayer();
    // log.info("Zug: {} Spieler: {}", gameState.getTurn(), currentPlayer.getColor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendAction(Move move) {
    client.sendMove(move);
  }

}
