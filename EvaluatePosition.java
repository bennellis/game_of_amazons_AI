package ubc.cosc322;
import java.util.*;

public class EvaluatePosition {
	//this variable is for iterating every direction a queen can move in. The first value is column direction, second
	//variable being row direction. ie 1, -1, means moving vertically up (1), and horizontally left (-1), so up left direction.
	public static final byte[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, };
	public byte[][] board = new byte[10][10];//global board variable, holds the game state. 0 means free spot
											 //1 means white queen, 2 means black queen, 3 means arrow
	
	// Movement is in this order: left, top-left, up, top-right, right, bottom-right, bottom, bottom-left
	public static final byte[][] Movement = { {0,-1}, {1,-1}, {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}};
	
	//distance and kingDistance are used in the minDistance and kingminDistance functions respectively
	public byte[][][] distance = new byte[10][10][2];//min dsitance to each board node, [0] is friendly, [1] is enemy
	public byte[][][] kingDistance = new byte[10][10][2];//min dsitance to each board node, [0] is friendly, [1] is enemy
	public byte[][] mobilityBoard = new byte[10][10];//each empty space of board has a value of how many immediately adjacent
													//empty squares.

	
	public byte[][] friendlyQueens = new byte[4][2];//locations of all friendly queens
	public byte[][] enemyQueens = new byte[4][2];//locations of all enemy queens
	public byte side;
	public Queue<byte[]> moves;//holds all legal queen moves in minDistance function
	public Queue<byte[]> kingMoves;//holds all legal king mvoes in kingMinDistance function
	public byte completedGameMoves;//how many moves have been made in the game so far
	public double w;//weight for weighting heuristics, changes based on a gamestate. This variable is only updated based
					//on the parent state, ie not updated based on future moves, only on the present actual moves that
				    //have been played. 
	//w starts at 70 at the beginning of the game, and as the game progresses typically goes down towards 0. it only
	//becomes 0 when all queens are isolated from oponent queens, meaning that no queen can reach an empty square that
	//an oponent queen can also reach.
	
	
	
	public double c1;//holds c1 heuristic value
	public double c2;//holds c2 heuristic value
	
	public boolean useMobility = true;//if we want to use mobility function, can change to false if we just want to use
									  //king and queen min distance heuristics
	public byte isOp;//if we are evaluating for our opponent, set to 1, if we are evaluating for ourselves, 0
	public static double K = 0; //this variable sets the value assigned in both minDistance functions in the case
							  	//that both players can reach an empty square in the same amount of moves < inf.
								//after experimentation we decided not to use this in our heuristics so set to 0.
	


	public EvaluatePosition(byte completedGameMoves) {//constructor needs how many game moves have been completed in the game so far
													  //CompletedGameMoves not actually used in final version.
		this.completedGameMoves = completedGameMoves;
	}
	//this is our evaluate function. takes the gameboard, friendly queens, enemyqueens, gameWeight, and isOp as inputs.
	//side variable is used to let us know if we are white or black. White = 1, black = 0.
	public int evaluate (byte[][] board, byte side, byte[][] friendlyQueens, byte[][] enemyQueens, double gameWeight, byte isOp) {
		for(int i = 0; i < 10; i++) {//initializes distance and kingDistance arrays to 127 (max byte value).
			for(int j = 0; j<10; j++) {
				distance[i][j][0] = 127;
				distance[i][j][1] = 127;
				kingDistance[i][j][0] = 127;
				kingDistance[i][j][1] = 127;
			}
		}
		//initialize variables
		int value = 0;
		this.board = board;
		this.friendlyQueens = friendlyQueens;
		this.enemyQueens = enemyQueens;
		this.side = side;
		this.moves = new LinkedList<byte[]>();
		this.kingMoves = new LinkedList<byte[]>();
		
		double queenint = minDistance();//evaluates minDistance and stores return in queenint variable (used to return int hence the name)
		double weightQueen = 0;//initialize weightQueen which is the weight for this heuristic
		double weightKing = 0;//weight for king heuristic
		double weightMobil = 0;//weight for mobility heuristic
		double weightC1 = 0;//weight for C1 heuristic
		double weightC2 = 0;//weight for c2 heuristic
		double kingint = 0;//initialize what will be returned from king min distance if called
		double MobilVal = 0;//initialize what will be returned from mobility function if called
		
		//the following code will determine how the heuristics are weighted as the game progresses and w changes.
		//queenmin, kingmin, C1, and C2 weights should sum to 1, and weightMobil should be < 1. as the game progresses,
		//queenmin will become increasingly valuable, and its weight will go up, whereas all other weights should
		//decrease to 0. when w < 10, only queenMin should be used, and no other heuristics need to be evaluated.
		
		
		if (useMobility) {//if we are using the mobility function
			if (gameWeight > 10) {//if the gameweight is greater than 10, if less than 10 we will only use queendistance
								  //function as it is basically in the endgame and no need to waste processing power
								  //on heuristics that will be weighted to 0 anyways.
				
				if (gameWeight > 60) {//if >60 meaning very early game, first 5-10 moves typically
					weightQueen = 0.4;//v1 0.1 v2 0.15
					weightKing = 0.4; //v2 0.5
					weightC1 = 0.1;
					weightC2 = 0.1;
					weightMobil = 0.6667;
				}
				else if (gameWeight > 55) {//if < 60 but > 55, meaning typically move 5 through 15 about
					weightQueen = 0.425;//v1 0.1 v2 0.225
					weightKing = 0.425;//v2 0.475
					weightC1 = 0.075;
					weightC2 = 0.075;
					weightMobil = 0.55;
				}

				else if (gameWeight > 50) {//if < 55 but > 50, meaning typically move 10 through 20 about
					weightQueen = 0.45;//v1 0.1 v2 0.275
					weightKing = 0.45; //v2 0.45
					weightC1 = 0.05;
					weightC2 = 0.05;
					weightMobil = 0.45;
				}
				else if (gameWeight > 45) {//if < 50 but > 45, meaning typically move 15 through 25 about
					weightQueen = 0.475;//v1 0.25 //v2 0.3
					weightKing = 0.475;//v1 0.4 //v2 0.45
					weightC1 = 0.025;
					weightC2 = 0.025;
					weightMobil = 0.35;
				} 
				
				else if (gameWeight > 40) {//getting closer to late game, typically after move 20
					weightQueen = 0.5;//v1 0.25 //v2 0.35
					weightKing = 0.5;//v1 0.4 //v2 0.4
					//weightC1 = 0.1;
					//weightC2 = 0.15;
					weightMobil = 0.275;
				} 
				else if (gameWeight > 30) {//close to end game, typically after move 30
					weightQueen = 0.55;//v1 0.4 //v2 0.4
					weightKing = 0.45;//v1 0.35 //v2 0.4
					//weightC1 = 0.1;
					//weightC2 = 0.1;
					weightMobil = 0.15;
				} else if (gameWeight > 20) {//starts to move very quickly as gameweight decreases below 30. Varies greatly per game
					weightQueen = 0.6;//v1 0.55 //v2 0.55
					weightKing = 0.4;//v1 0.3 //v2 0.35
					//weightC1 = 0.05;
					//weightC2 = 0.05;
					weightMobil = 0.05;
				} else if (gameWeight > 10) {//less than 20 greater than 10, very close to filling game
					weightQueen = 0.75;//v1 0.75
					weightKing = 0.25;//v1 0.15
					weightC1 = 0;
					weightC2 = 0;
					weightMobil = 0;
				} else { //this isnt in use, as it shouldn't reach this point see higher if statement.
					weightQueen = 0.9;//v1 0.9
					weightKing = 0.1;//v1 0.1
					weightC1 = 0;
					weightC2 = 0;
					weightMobil = 0;
				}
				//weightMobil = 1.0 - 2.0 * weightQueen;
				//weightKing = weightQueen;

				kingint = kingMinDistance();//evaluates kingMinDistance here, only if gameweight is >10 and we are using
											//mobility
				MobilVal = mobilityFunc();//evaluates mobility here, only if gameweight is >10 and we are using
										  //mobility
				// System.out.println("EARLY GAME");
				// System.out.println("w = " + this.w);
			} else {//if w less than 10, all weights remain 0 and weightQueen set to 1.
				weightQueen = 1;
				weightKing = 0;
				// System.out.println("END GAME");
			}
		}
		else {//if we are not using mobility
			if (gameWeight != 0) {//if not endgame
				kingint = kingMinDistance();//evaluate king distance
				weightQueen = 0.5;//split king and queen weight equally
				weightKing = 0.5;
				weightMobil = 0;
			}
			else {//if endgame only use queen distance no need to evaluate king distance.
				weightQueen = 1;
				weightKing = 0;
				weightMobil = 0;
			}
		}

		

//		int QueenValue = 0;
		//following code returns the weighted sum of all the heuristics. It rounds it to an Int.
		// one further improvement would be making this all a double and not rounding, as some precision is lost.
		double QueenDouble = weightQueen*(double)queenint;
		double KingDouble = weightKing*(double)kingint;
		double mobilDouble = weightMobil*MobilVal;
		double c1Double = weightC1*this.c1;
		double c2Double = weightC2*this.c2;
		int returnVal = (int) Math.round(QueenDouble+KingDouble+mobilDouble +c1Double + c2Double);
		
		
		return returnVal;
	}
	//calculates the queen min distance heuristic. It determines how many queen moves it will take minimum for each side to
	//reach an empty square, and then whoever can reach it in the least amount of moves "owns" that square and is awarded
	//1 point, (-1 for enemy).
	private double minDistance() {
		double value = 0;//value to return
		
		
		//this for loop is for deciding whether to do friendly queens or enemy queens. starts with friendly and then
		//evaluates enemy.
		for(byte i = 0; i < 2; i++) {
			boolean allDone = false;//this variable checks if we can break the loop, I think it is redundant in recent updates.
			byte[][] tempQueens = new byte[4][3];//temp variable to hold queens to evaluate, friendly or enemy. 
			//third variable is amount of moves done so far in this current move
			for(byte j = 0; j < 4; j++) {
				if (i == 0) {//if we are evaluating friendly queens right now
					tempQueens[j][0] = friendlyQueens[j][0];
					tempQueens[j][1] = friendlyQueens[j][1];
					tempQueens[j][2] = 0;//this third value is for how many moves have been evaluated so far, used later
				} else if (i == 1) {//if enemy queens
					tempQueens[j][0] = enemyQueens[j][0];
					tempQueens[j][1] = enemyQueens[j][1];
					tempQueens[j][2] = 0;//how many moves have been evaluated so far
				}
				//System.out.println(tempQueens[j][0] + " " + tempQueens[j][1] + " " + tempQueens[j][2]);
			}

			//add starting positions to search queue. This queue will be used to search every possible move a queen can
			//make. It is structured so that we will only add a move to the queue if it has not already been searched, and
			//we will evaluate all 1 moves before any 2 moves, and all 2 moves before any 3 moves to make this search
			//as efficient as possible.
			
			moves.add(tempQueens[0]);//adds the first queen location with a move count of 0
			moves.add(tempQueens[1]);//adds the second queen location with a move count of 0
			moves.add(tempQueens[2]);//adds the third queen location with a move count of 0
			moves.add(tempQueens[3]);//adds the fourth queen location with a move count of 0
			
			for(byte[] Queen : tempQueens) {
				//set initial queen positions to be value 0, meaning it can reach those squares in 0 moves and as such
				//these spaces are marked as visited, and will not be re-added to search queue.
				distance[Queen[0]][Queen[1]][i] = 0;
			}
			while(!allDone) {//this is redundant now, could have just used the next whileloop but was used in earlier iteration.
				
				allDone = true;
				//for every queen of side
				byte[] Move;
				while(!moves.isEmpty()) {//while items exist in search queue
					
					Move = moves.remove();//remove top move from search queue
					
					//calculate min distance in one hop
					for(byte[] direction : directions) {//for every direction, ie left, right, up right, etc.
						boolean noChanges = false;
						//the following function will add new moves to the search queue with +1 #of moves it took to get
						//to that squre, if the square has not been reached yet, and then sets the square to have been reached
						//in #of moves it took to get there.
						noChanges = calculateQueenDistance(Move, direction, i);
						if(allDone == true && noChanges == false)//I think this is redundant now.
							allDone = false;
					}
				}	
			}
		}
		
		//the following function calculates who owns each square, and will also evaluate w and C1

		double[][] Territories = ownedTerritories(distance, true);
		
		//calculate difference in owned squares
		for(int j = 0; j < 10; j++) {
			for(int k = 0; k < 10; k++) {
				value += Territories[j][k];	
			}
		}

		
		return value;//returns difference in owned squares
	}
	//old mobility function, not used in final program.
	private double mobility() {
		double value = 0;
		int friendlyMoves = 0;
		int opponentMoves = 0;
		
		for (byte[] queen : friendlyQueens) {
			friendlyMoves += getLegalMoves(queen);
		}
		for (byte[] queen : enemyQueens) {
			opponentMoves += getLegalMoves(queen);
		}
		
		value = friendlyMoves - opponentMoves;
		
		return value;
	}
	//used in the old mobility function, not used in final program
	private double getLegalMoves(byte[] queen) {
		// we start out from one because it is a position that the queen can go to. But since we don't want
		// the queen to go there every single time, we set the queen position to one, 
		double out = 1;
		
		byte[][] tempBoard =  new byte[10][10];
		
		for ( int q = 0; q < 10; q++) {
			for (int w = 0; w < 10; w++) {
				tempBoard[q][w] = this.board[q][w];
			}
		}
		byte[][] tempMove = Movement;
		// this will store all the positions we will visit in one move. 0th column will be row, 1st column 
		// will be the column
		List<byte[]> allMoves = new ArrayList<>();
		
		//we set the current position of the queen to be one so that no squares ever go there. 
		
		tempBoard[queen[0]][queen[1]] = 1;
		
		for (int i = 0; i < 8; i++) {
			byte x = queen[0];
			byte y  = queen[1];
			
			byte dirX = tempMove[i][0];
			byte dirY = tempMove[i][1];
			
			x += dirX;
			y += dirY;
			
			while (x >= 0 && x < 10 && y >= 0 && y < 10 && tempBoard[x][y] == 0) {
				// add one to the total since this is a new block.
				out += 1;
				// mark it as reached
				tempBoard[x][y] = 1;
				// store the coordinates in the reached arraylist - on second thought I don't think we need
				// to do this because all the possible positions from here will already be calculated with 
				// the next for loop. But I am going to keep it for now.
				byte [] curr = {x,y};
				
				allMoves.add(curr);

				
				x += dirX;
				y += dirY;
			}
		}
		
		// this for loop goes through all of the recorded moves, and calculate any moves from there
		// this helps us calculate all of the locations we can reach on a second move from the current location
		
		for (int k = 0; k < allMoves.size(); k++) {
			byte[] move = allMoves.get(k);
			
			byte moveX = move[0];
			byte moveY = move[1];
			
			byte directionX = tempMove[k][0];
			byte directionY = tempMove[k][1];
			
			// we temporarily give the current square a value of 0 so that it enters the while loop.
			
			
			
			// we use this boolean to check if the current square is the starting square
			// if it is , then we don't add 1 to out, because it is already accounted for
			
			moveX += directionX;
			moveY += directionY;
			
			
			while (moveX >= 0 && moveX < 10 && moveY >= 0 && moveY < 10 && tempBoard[moveX][moveY] == 0) {
				
	
				
				out += 0.75;
				
				
				tempBoard[moveX][moveY] = 1;
				
				
				moveX += directionX;
				moveY += directionY;
				
			}
			
		}
		
		
		return out;
		
	}
	//this is the mobility function used in the final program.
	//This function basically works by assigning a value to each empty square in the game board representing how many
	//adjacent empty squares it has, and then for every queen gets a "mobility" value based on the sum of all of these
	//values it can reach in a single queen move, however for every king move away the value is reduced by 1/2.
	//it then uses these mobility values for each queen to calculate a penalty for less mobile queens. A completely
	//enclosed queen gets a penalty of -15 (to be weighted in the evaluate function), and an almost enclosed queen gets
	// a penalty of -5, and then it slowly increases from there as the queen gets more mobile. However no benefits are
	//ever given to a more mobile queen, only penalties to less mobile queens. This lets our AI aggressively try to
	//enclose and trap opponent queens, and keep it's own queens from being trapped in the early game (when this function
	//is weighted highest).
	
	
	private double mobilityFunc() {
		double value = 0;
		//the following double four loop adds a assigns value for each empty square in the global game board to the variable
		//mobilityBoard. This value represents how many empty adjacent squares exist to this empty square, and is used
		//in the mobility function.
		for(int i = 0; i < 10; i++) {
			//System.out.println();
			for(int j = 0; j < 10; j++) {
				mobilityBoard[i][j] = 0;//initializes the mobilityBoard
				if(board[i][j]==0) {//if this square is empty
					for(byte[] direction : directions) {//for every adjacent square (basically)
						int c = i + direction[0];
						int r = j + direction[1];
						if(c<10&&c>=0&&r<10&&r>=0&&board[c][r]==0) {//if it is a valid square, and is empty
							mobilityBoard[i][j]++;//increment the value for the mobilityBoard.
						}
					}
				}
			}
		}
		//copied and edited from minDistance
		for(byte i = 0; i < 2; i++) {
			byte[][] tempQueens = new byte[4][3];
			//third variable is amount of moves done so far in this current move
			for(byte j = 0; j < 4; j++) {
				if (i == 0) {
					tempQueens[j][0] = friendlyQueens[j][0];
					tempQueens[j][1] = friendlyQueens[j][1];
					tempQueens[j][2] = 0;
				} else if (i == 1) {
					tempQueens[j][0] = enemyQueens[j][0];
					tempQueens[j][1] = enemyQueens[j][1];
					tempQueens[j][2] = 0;
				}
				//System.out.println(tempQueens[j][0] + " " + tempQueens[j][1] + " " + tempQueens[j][2]);
			}

//			int queenNum = 0;
			for(byte[] Queen : tempQueens) {//for each queen in the tempQueens array
				double QueenVal = 0;
				for(byte[] direction : directions) {//for every direction
					int c = Queen[0] + direction[0];
					int r = Queen[1] + direction[1];
					int count = 0;
					//while the new position is within the gameboard, the new square is free, and both players can reach the square
					//in <127 (max byte value) queen moves
					while(c<10&&c>=0&&r<10&&r>=0&&board[c][r]==0&&distance[c][r][0]!=127&&distance[c][r][1]!=127) {
						//sum the mobility board value reduced by 1/2 for every king move to reach this square.
						QueenVal += mobilityBoard[c][r]*Math.pow(2, Math.negateExact(count));
						
						
						count++;
						c+=direction[0];
						r+=direction[1];
					}

				}
				
//				System.out.println("Queen " + queenNum + "Team " + i + " val: " + QueenVal);
//				queenNum++;
				double toAdd = 0;
				if(QueenVal>20) {//if queenVal is > 20 meaning a very mobile queen
					toAdd=QueenVal/40.0-2.5;//results in -2 penalty at 20, and 0 at 100 (typically the highest mobility).
				}

				else if(QueenVal>5&&QueenVal<=20) {//if queenVal is >5 and less than 20, not very mobile
					toAdd = QueenVal/5.0-6.0;//results in -5 penalty at 5, and -2 at 20, linearly distributed
				}
				else if(QueenVal<=5 && QueenVal>=0) {//if queenVal is less than 5, meaning almost trapped / completely trapped
													 //at 0.
					toAdd = QueenVal*2.0-15.0;//results in a penalty of -5 at 5, and -15 at 0.
				}

				if(i==0) {//if evaluating for myself sum the penalties for each queen
					value+=toAdd;
				}
				else {//if evaluating for my opponent, subtract the penalties for each queen
					value-=toAdd;
				}
			}
		}
		//end of copy
		
		return value;
	}
	
	//This heuristic is for determining the king min distance. It basically determines how many King moves it takes each
	//player minimum to reach any empty square, and then compares who can reach it quickest. Whoever can reach it quickesat
	//is determined to "own" that square and is awarded a point (-1 for opponent). all squares are summed to determine the
	//value of this heuristic. Very similar to queen min distance, just only evaluates one move in any direction before
	//adding 1 to the #of moves to reach that square and returning.
	private double kingMinDistance() {
		double value = 0;
		
		//System.out.println("MOBILITY BOARD");

		//System.out.println();
		//this for loop is for deciding whether to do friendly queens or enemy queens.
		for(byte i = 0; i < 2; i++) {
			boolean allDone = false;
			byte[][] tempQueens = new byte[4][3];
			//third variable is amount of moves done so far in this current move
			for(byte j = 0; j < 4; j++) {
				if (i == 0) {
					tempQueens[j][0] = friendlyQueens[j][0];
					tempQueens[j][1] = friendlyQueens[j][1];
					tempQueens[j][2] = 0;
				} else if (i == 1) {
					tempQueens[j][0] = enemyQueens[j][0];
					tempQueens[j][1] = enemyQueens[j][1];
					tempQueens[j][2] = 0;
				}
				//System.out.println(tempQueens[j][0] + " " + tempQueens[j][1] + " " + tempQueens[j][2]);
			}

			//add starting positions to search queue
			
			kingMoves.add(tempQueens[0]);
			kingMoves.add(tempQueens[1]);
			kingMoves.add(tempQueens[2]);
			kingMoves.add(tempQueens[3]);
			
			for(byte[] Queen : tempQueens) {
				//set initial queen positions to be value 0
				kingDistance[Queen[0]][Queen[1]][i] = 0;
			}
			//int debugcounter = 0;
			while(!allDone) {//redundant in recent update
				//debugcounter++;
				allDone = true;
				//for every queen of side
				byte[] Move;
				while(!kingMoves.isEmpty()) {
					
					Move = kingMoves.remove();
					
					//calculate min distance in one hop
					for(byte[] direction : directions) {
						boolean noChanges = false;
						noChanges = calculateKingDistance(Move, direction, i);//other than this function, I believe everything
						//is the same in the KingMinDistance compared to the QueenMinDistance heuristic.
						if(allDone == true && noChanges == false)//redundant
							allDone = false;
					}
				}
				//if(debugcounter>20)
					//break;
			}
		}
		
		//calculate who owns each square
		//System.out.println();
		double[][] Territories = ownedTerritories(kingDistance, false);//also calculates C2 heuristic
		
		//calculate difference in owned squares
		for(int j = 0; j < 10; j++) {
			for(int k = 0; k < 10; k++) {
				value += Territories[j][k];	
			}
		}
		return value;
	}
	//this function is used in KingMinDistance heuristic
	private boolean calculateKingDistance(byte[] Queen, byte[] direction, byte side) {
		byte c = (byte) (Queen[0] + direction[0]); 
		byte r = (byte) (Queen[1] + direction[1]);
		boolean isDone = true;
		
		
		//while position is inbounds, and free in the baord
		if(c < 10 && c >=0 && r < 10 && r >=0 && board[c][r] == 0) {
			//System.out.print(c + " " + r + ", ");
			
			if (kingDistance[c][r][side] == 127) {//if square has not yet been reached
				
				byte[] nextMove = new byte[3];
				nextMove[0] = c;
				nextMove[1] = r;
				nextMove[2] = (byte) (Queen[2]+1);//adds one to # of moves to reach this square
				kingDistance[c][r][side] = nextMove[2];//sets this square to be able to be reached in nexMove[2] # of moves
				kingMoves.add(nextMove);//adds this square to search queue
				isDone = false;
			}
			c+= direction[0];//redundant copy and paste from queen min distance function
			r+= direction[1];//redundant copy and paste from queen min distance function
		}
		return isDone;
	}
	
	//this function is used in MinDistance heuristic
	private boolean calculateQueenDistance(byte[] Queen, byte[] direction, byte side) {
		byte c = (byte) (Queen[0] + direction[0]); 
		byte r = (byte) (Queen[1] + direction[1]);
		boolean isDone = true;
		
		
		//while position is inbounds, and free in the baord
		while(c < 10 && c >=0 && r < 10 && r >=0 && board[c][r] == 0) {
			//System.out.print(c + " " + r + ", ");
			if (distance[c][r][side] == 127) {//if empty square has not yet been reached
				byte[] nextMove = new byte[3];
				nextMove[0] = c;
				nextMove[1] = r;
				nextMove[2] = (byte) (Queen[2]+1);//increment number of moves it took to reach this square
				distance[c][r][side] = nextMove[2];//set the square to be able to be reached in nextMove[2] moves
				moves.add(nextMove);//add this move to the earch queue
				isDone = false;
			}
			//try to move in this direction one more step and go through while again
			c+= direction[0];
			r+= direction[1];
		}
		return isDone;
	}
	
	//this function is used to determine who owns each squre based on how many moves it takes each player to reach
	//that square in the queenminDistance and KingMinDistance functions.
	//It uses variables dis (being the triple array of how many moves it takes each player to reach each square in the
	//game board in the king/queen min distance, and the boolean isD1 being true if this is being called from queenMin
	//and false if it is being called from queenMin.
	
	//Heuristics c1, c2, and wight variable w are also determined in this function.
	//C1 is used to give a higher value to queenMinDistance values that one player can reach in much less moves than the
	//opponent, and less of a value if they can almost reach them in the same amount of moves.
	//C2 is the same as C1 except for king moves.
	//W is determined based on how closed up the gameboard is, starts at 70 and decreases to 0 as the game goes on.
	//a value of 0 means that there are no empty squares that both players can reach.
	private double[][] ownedTerritories(byte[][][] dis, boolean isD1){
		double[][] Territory = new double[10][10];
		double weight = 0;
		double c1val = 0;
		double c2val = 0;
		//System.out.println();
		
		//for every square in the game board
		for(int i = 0; i < 10; i++) {
			for(int j = 0; j < 10; j++) {
				if(isD1) {//if called from queen min distance, evaluate w and c1
					if(dis[i][j][0]!=127&&dis[i][j][1]!=127) {//if both players can reach the square
						//sum the following, 2 to the power of the negative absolute value of the difference of number of
						//queen moves it takes each player to reach an empty square
						weight+= Math.pow(2.0, Math.negateExact(Math.abs(dis[i][j][0]-dis[i][j][1])));
						
					}
					//c1 is the sum of each square, 2*(2 to the power of negative my queen moves to reach the square, minus 2
					//to the power of negative opponent moves to reach the square.
					c1val+=2.0*(Math.pow(2.0, Math.negateExact(dis[i][j][0])) - Math.pow(2.0, Math.negateExact(dis[i][j][1])));
				}
				else {//if called from king min distance function
					//c2 is the sum of the the following:
					//take the maximum of -1 and the difference of opponent king moves to that square minus my king moves
					//to that square divided by 6. and then take the minimum of 1 and that value.
					c2val+=Math.min(1.0, Math.max(-1.0, ((double)(dis[i][j][1]-dis[i][j][0]))/6.0));
					
				}
				//System.out.print(dis[i][j][0] + " ");//for debugging
				//if we owns the squre being that it can reach it in shorter time than oponent
				if(dis[i][j][0] < dis[i][j][1])
					Territory[i][j] = 1;
				//else if oponent owns the square
				else if (dis[i][j][0] > dis[i][j][1])
					Territory[i][j] = -1;
				//else if we tie on the amount of time to reach a square
				else if(dis[i][j][0] == dis[i][j][1]&&dis[i][j][0]==127) {//if we both can't reach the square
					Territory[i][j] = 0;
				}
				else if(dis[i][j][0] == dis[i][j][1]) {//if it is a tie on time to reach square
					if(isOp==0) {
						Territory[i][j] = -K;
					}
					else if (isOp==1) {
						Territory[i][j] = K;
					}
					else {
						System.out.println("ERROR IN OWNEDTERRITORIES");//for debugging if there is an issue for some reason.
					}
				}
			}
			//System.out.println();
		}
		if(isD1) {//sets w and c1
			this.w = weight;
			this.c1 = c1val;
		}
		else {//sets c2
			this.c2 = c2val;
		}
		return Territory;
	}
}
