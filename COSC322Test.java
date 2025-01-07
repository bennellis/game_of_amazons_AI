
package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;
import ygraph.ai.smartfox.games.amazons.HumanPlayer;

/**
 * An example illustrating how to implement a GamePlayer
 * 
 * @author Yong Gao (yong.gao@ubc.ca) Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer {
	
	private static byte SEARCH_DEPTH = 2;//when using static depth alpha beta, we can set the depth to search here

	private GameClient gameClient = null;
	private BaseGameGUI gamegui = null;

	private String userName = null;
	private String passwd = null;
	public byte[][] board = null;//global gameboard
	public byte[][] friendlyQueens = new byte[4][2];//hold position of every friendly queen
	public byte[][] enemyQueens = new byte[4][2];//holds position of every enemy queen
	public byte completedGameMoves = 0;//keeps track of how many moves have been completed, currently not used but have
									   //it in case we wanted to use it for weighting our heuristics
	
	public byte isWhite = 0;//is 1 if we are playing as white, 0 if we are black
	public static boolean isAI = true;//change this to false if you want to play as human player

	/**
	 * The main method
	 * 
	 * @param args for name and passwd (current, any string would work)
	 */
	public static void main(String[] args) {
		if (isAI) {//if we are using our AI player
			COSC322Test player = new COSC322Test(args[0], args[1]);
			System.out.println("Creating Ai Player");

			if (player.getGameGUI() == null) {
				player.Go();
			} else {
				BaseGameGUI.sys_setup();
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						player.Go();
					}
				});
			}
		}
		else {//if we are using the human player
			HumanPlayer player = new HumanPlayer();
			System.out.println("creating human player");
			if (player.getGameGUI() == null) {
				player.Go();
			} else {
				BaseGameGUI.sys_setup();
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						player.Go();
					}
				});
			}
		}
		
	}

	/**
	 * Any name and passwd
	 * 
	 * @param userName
	 * @param passwd
	 */
	public COSC322Test(String userName, String passwd) {
		this.userName = userName;
		this.passwd = passwd;

//To make a GUI-based player, create an instance of BaseGameGUI
//and implement the method getGameGUI() accordingly
		this.gamegui = new BaseGameGUI(this);
	}

	@Override
	public void onLogin() {
		userName = gameClient.getUserName();
		if (gamegui != null) {
			gamegui.setRoomInformation(gameClient.getRoomList());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
//This method will be called by the GameClient when it receives a game-related message
//from the server.

//For a detailed description of the message types and format,
//see the method GamePlayer.handleGameMessage() in the game-client-api document.
		switch (messageType) {
		case GameMessage.GAME_ACTION_START://on game start, initialize if we are white or black, and if black make first move.
			if (msgDetails.get("player-white").equals(this.userName)) {//if we are white, set isWhite to 1 and build the
																	   //initial game board
				System.out.println("we are white");
				this.isWhite = 1;
				this.board = buildBoard();//builds initial game board
			}
			else {//if we are black
				System.out.println("We are black");
				this.isWhite = 0;
				this.board = buildBoard();//builds initial game board
				if(isAI) {//if we are AI (this is redundant as if it reaches this point we will be using AI player)
					//the following code is for when we wanted to use static depth alpha beta rather than iterative deepening.
					
//					AlphaBetaSearch ourplayer = new AlphaBetaSearch(board, isWhite, SEARCH_DEPTH, friendlyQueens,
//							enemyQueens, completedGameMoves);
//					completedGameMoves++;
//
//					Action bestAction = ourplayer.decideMove();//This is for static depth alpha beta
					
					
					//this is for our iterative deepening player
					IterativeDeepening ourplayer = new IterativeDeepening(board, isWhite, SEARCH_DEPTH, friendlyQueens,
							enemyQueens, completedGameMoves);
					Action bestAction = ourplayer.iterate();
					
					ourplayer.makeMove(bestAction);
					//makes the move on the global gameboard based on what our iterative deepening player says is best
					
					
					//prints the move made
					System.out.println("old queen (" + bestAction.prev[0] + ", " + bestAction.prev[1] + ")");
					System.out.println("new queen (" + bestAction.curr[0] + ", " + bestAction.curr[1] + ")");
					System.out.println("new arrow (" + bestAction.arrow[0] + ", " + bestAction.arrow[1] + ")");

					
					//following code is for sending the move chosen to game server
					ArrayList<Integer> A1 = new ArrayList<>();
					ArrayList<Integer> B1 = new ArrayList<>();
					ArrayList<Integer> C1 = new ArrayList<>();

					//following converts the move from our coordinate system to the game server coordinate system
					A1.add((int) (10 - bestAction.prev[0]));
					A1.add((int) (bestAction.prev[1] + 1));

					B1.add((int) (10 - bestAction.curr[0]));
					B1.add((int) (bestAction.curr[1] + 1));

					C1.add((int) (10 - bestAction.arrow[0]));
					C1.add((int) (bestAction.arrow[1] + 1));

					gameClient.sendMoveMessage(A1, B1, C1);//sends the arraylists to game server
					this.getGameGUI().updateGameState(A1, B1, C1);//updates gui

					ourplayer.displayBoard();//prints global board
				}
			}
			
			break;
			
			
		case GameMessage.GAME_STATE_BOARD:
			this.getGameGUI().setGameState((ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE));
			//System.out.println("Game state board ben hi");
			
			break;
		case GameMessage.GAME_ACTION_MOVE://on oponent move
			this.getGameGUI().updateGameState(msgDetails);//gets opponents move from game server
			System.out.println("Game Action Move");
			System.out.println(msgDetails);//for debugging
			completedGameMoves++;//increments gamemoves
			
			if(isAI) {//reduntant, if gets to this point will be our ai player

				
				//following code is to get the opponent move from game server
				ArrayList<Object> A = new ArrayList<>();
				ArrayList<Object> B = new ArrayList<>();
				ArrayList<Object> C = new ArrayList<>();
				byte[] oldQueen = new byte[2];
				byte[] newQueen = new byte[2];
				byte[] newArrow = new byte[2];

				A = (ArrayList<Object>) msgDetails.get("queen-position-current");
				// It did not like a direct cast to byte, so casted to int first, and then to
				// byte.
				int x = (int) A.get(0);
				int y = (int) A.get(1);
				oldQueen[0] = (byte) (10 - x);// converts from server board to my board coordinate system
				oldQueen[1] = (byte) (y - 1);
				B = (ArrayList<Object>) msgDetails.get("queen-position-next");
				x = (int) B.get(0);
				y = (int) B.get(1);
				newQueen[0] = (byte) (10 - x);
				newQueen[1] = (byte) (y - 1);
				C = (ArrayList<Object>) msgDetails.get("arrow-position");
				x = (int) C.get(0);
				y = (int) C.get(1);
				newArrow[0] = (byte) (10 - x);
				newArrow[1] = (byte) (y - 1);
				//creates an action of oponents move in our coordinate system
				Action oponentMove = new Action(oldQueen, newQueen, newArrow);
				//prints move to user
				System.out.println("old queen op (" + oldQueen[0] + ", " + oldQueen[1] + ")");
				System.out.println("new queen op (" + newQueen[0] + ", " + newQueen[1] + ")");
				System.out.println("new arrow op (" + newArrow[0] + ", " + newArrow[1] + ")");

				
				//this is for static depth alpha beta player
//				AlphaBetaSearch ourplayer = new AlphaBetaSearch(board, isWhite, SEARCH_DEPTH, friendlyQueens,
//						enemyQueens, completedGameMoves); //to use set depth alpha beta
				
				
				// this is for iterative deepening player
				IterativeDeepening ourplayer = new IterativeDeepening(board, isWhite, SEARCH_DEPTH, friendlyQueens,
						enemyQueens, completedGameMoves);
				completedGameMoves++;//increments completed gamemoves
				if(!ourplayer.wasValid(oponentMove)){//if oponents move is not legal, prints it for user to detect
					System.out.println("CRIME! CRIME! CRIME! ILLEGAL MOVE DETECTED!");
				}
				// ourplayer.displayBoard();
				ourplayer.makeMove(oponentMove);//makes opponents move.
				// ourplayer.displayBoard();
				Action bestAction = ourplayer.iterate();//finds best move using our iterative deepening player
				//Action bestAction = ourplayer.decideMove(); //to use set depth alpha beta
				ourplayer.makeMove(bestAction);//makes move on global game board
				//prints best move to console
				System.out.println("old queen (" + bestAction.prev[0] + ", " + bestAction.prev[1] + ")");
				System.out.println("new queen (" + bestAction.curr[0] + ", " + bestAction.curr[1] + ")");
				System.out.println("new arrow (" + bestAction.arrow[0] + ", " + bestAction.arrow[1] + ")");


				//following code is for sending our chosen best move to the game server
				ArrayList<Integer> A1 = new ArrayList<>();
				ArrayList<Integer> B1 = new ArrayList<>();
				ArrayList<Integer> C1 = new ArrayList<>();

				//converts from our coordinate system to gameserver coordinate system
				A1.add((int) (10 - bestAction.prev[0]));
				A1.add((int) (bestAction.prev[1] + 1));

				B1.add((int) (10 - bestAction.curr[0]));
				B1.add((int) (bestAction.curr[1] + 1));

				C1.add((int) (10 - bestAction.arrow[0]));
				C1.add((int) (bestAction.arrow[1] + 1));

				gameClient.sendMoveMessage(A1, B1, C1);//sends move to server
				this.getGameGUI().updateGameState(A1, B1, C1);//updates gui
				ourplayer.displayBoard();//prints global gameboard to console
			}
			
			break;
		default:
			System.out.println("Game default");
			assert (false);
			
			break;
		}

		return true;
	}
	
	@SuppressWarnings("unchecked")
	
	//this function is not used in final version. was used previously to build the global gameboard based on the msg content
	//but now we just initialize it to what it should be at beginning of game and only change it when moves are made.
	public byte[][] buildBoard(Map<String, Object> msgDetails) {
		byte[][] board = new byte[10][10];
		ArrayList<Integer> al = new ArrayList<Integer>();
		al = (ArrayList<Integer>) msgDetails.get("game-state");
		
		//setting all empty locations to be 0's
		//black queens are 2's
		//white queens are 1's
		//arrows are 3's
		int value = 0;
		int countFriendlies = 0;
		int countEnemies = 0;
		for (byte i = 0; i < 10; i++) {
			for(byte j = 0; j < 10; j++) {
				value = al.get((i+1)*11+j+1);
				board[i][j] = (byte) value;
				if (value == (this.isWhite+1)) {
					this.enemyQueens[countEnemies][0] = i;
					this.enemyQueens[countEnemies][1] = j;
					countEnemies++;
				}
				else if (value == (2 - this.isWhite)) {
					this.friendlyQueens[countFriendlies][0] = i;
					this.friendlyQueens[countFriendlies][1] = j;
					countFriendlies++;
				}
				System.out.print(board[i][j]+" ");
			}
			System.out.println();
		}
		System.out.println();
		displayQueens();

		
			
		return board;
	}
	
	//initializes global gameboard to start state, and initializes friendly and enemy queens based on if we are white or black
	public byte[][] buildBoard() {
		byte[][] board = new byte[10][10];
		//initialize board to be all zeros
		for(byte i = 0; i < 10; i++) {
			for(byte j = 0; j < 10; j++) {
				board[i][j] = 0;
			}
		}
		//add queens
		
		if(this.isWhite==0) {
			this.friendlyQueens[0][0] = 0;
			this.friendlyQueens[0][1] = 3;
			this.friendlyQueens[1][0] = 0;
			this.friendlyQueens[1][1] = 6;
			this.friendlyQueens[2][0] = 3;
			this.friendlyQueens[2][1] = 0;
			this.friendlyQueens[3][0] = 3;
			this.friendlyQueens[3][1] = 9;
			
			this.enemyQueens[0][0] = 6;
			this.enemyQueens[0][1] = 0;
			this.enemyQueens[1][0] = 6;
			this.enemyQueens[1][1] = 9;
			this.enemyQueens[2][0] = 9;
			this.enemyQueens[2][1] = 3;
			this.enemyQueens[3][0] = 9;
			this.enemyQueens[3][1] = 6;
		}
		else if (this.isWhite==1) {
			this.friendlyQueens[0][0] = 6;
			this.friendlyQueens[0][1] = 0;
			this.friendlyQueens[1][0] = 6;
			this.friendlyQueens[1][1] = 9;
			this.friendlyQueens[2][0] = 9;
			this.friendlyQueens[2][1] = 3;
			this.friendlyQueens[3][0] = 9;
			this.friendlyQueens[3][1] = 6;
			
			this.enemyQueens[0][0] = 0;
			this.enemyQueens[0][1] = 3;
			this.enemyQueens[1][0] = 0;
			this.enemyQueens[1][1] = 6;
			this.enemyQueens[2][0] = 3;
			this.enemyQueens[2][1] = 0;
			this.enemyQueens[3][0] = 3;
			this.enemyQueens[3][1] = 9;
		}
		//initialize board with queens, black queen is 2 white queen is 1
		
		board[0][3] = 2;
		board[0][6] = 2;
		board[3][0] = 2;
		board[3][9] = 2;
		board[6][0] = 1;
		board[6][9] = 1;
		board[9][3] = 1;
		board[9][6] = 1;

		displayQueens();
		return board;
	}
	
	public void evaluateState() {
		
	}
	
		
	//this just prints friendly and enemy queens for debugging
	public void displayQueens() {
		System.out.println("Friendly queens:");
		for (int i = 0; i < 4; i++) {
			System.out.print("("+this.friendlyQueens[i][0] + ", " + this.friendlyQueens[i][1] + "), ");
		}
		System.out.println();
		System.out.println("Enemy queens:");
		for (int i = 0; i < 4; i++) {
			System.out.print("("+this.enemyQueens[i][0] + ", " + this.enemyQueens[i][1] + "), ");
		}
		System.out.println();
	}

	@Override
	public String userName() {
		return userName;
	}

	@Override
	public GameClient getGameClient() {
// TODO Auto-generated method stub
		return this.gameClient;
	}

	@Override
	public BaseGameGUI getGameGUI() {
// TODO Auto-generated method stub
		return this.gamegui;
	}

	@Override
	public void connect() {
// TODO Auto-generated method stub
		gameClient = new GameClient(userName, passwd, this);
	}

}// end of class
