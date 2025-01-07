package ubc.cosc322;
import java.util.*;

public class AlphaBetaSearch extends Player{
	
	public Action bestMove; //the best move decided by this search
	public static int moveCheckCutoff = Integer.MAX_VALUE; //this variable is to speed up search tree with priority queue moves
											//by only continuing searching with x best previous moves.
	public long startTime; //the startime (used by iterative deepening for cutoff time)
	public long timeout; //timeout (used by iterative deepening for cutoff time)
	public double gameWeight; //this is used to weight the heuristic functions in the evaluate class
	
	//initializes values, and sets startime and timeout to be -1
	public AlphaBetaSearch(byte[][] board, byte side, byte searchDepth, byte[][] friendlyQueens, byte[][] enemyQueens, byte completedGameMoves) {
		super(board, side, searchDepth, friendlyQueens, enemyQueens, completedGameMoves);
		this.startTime = -1;
		this.timeout = -1;
	}
	//if no startime and timeout is passed as arguments, they will remain as -1.
	//if iterative deepening is being used this function is never called.
	public Action decideMove() {
		
		maximizer(super.searchDepth, Integer.MIN_VALUE , Integer.MAX_VALUE);
		
		displayBoard();
		return bestMove;
		
		
	}
	//if startime and timeout are passed as arguments, it will set their values here
	public Action decideMove(long startTime, long timeout) {

		this.startTime = startTime;
		this.timeout = timeout;
		//the gameboard is evaluated before searching so we can use the gameWeight (used for heuristic weighting) of
		//the initial gameboard, rather than of each of the child boards.
		EvaluatePosition eval = new EvaluatePosition(completedGameMoves);
		eval.evaluate(super.board, super.side, super.friendlyQueens, super.enemyQueens, 0, (byte)0);
		this.gameWeight = eval.w;
		//System.out.println("GAME WEIGHT: " + gameWeight);
		maximizer(super.searchDepth, Integer.MIN_VALUE , Integer.MAX_VALUE);
		
		//displayBoard();
		return bestMove;
		
		
	}
	
	public int maximizer(byte depth, int alpha, int beta) {
		
		if (depth == 0) {//if it is depth 0 meaning that it is a leaf node, just evaluate the position and return to parent.
			EvaluatePosition eval = new EvaluatePosition(completedGameMoves);
			return(eval.evaluate(super.board, super.side, super.friendlyQueens, super.enemyQueens, this.gameWeight, (byte) 1));
		}
		//we experimented with only continuing branches that were in the x best moves to allow for a deeper but narrower search
		//however we determined for our final bot not to use this. 
		boolean useCutoff = false;//if depth is 1 we still need to evaluate all moves, but if greater than 1 we can use cutoff
		byte isOp = 0; //to pass to functions to determine if we are evaluating a position for ourselves or our oponnent
		int cutoffCounter = 0;
		//pre sorting the moves if it is not the last depth
		Queue<Action> legalMoves = new LinkedList<>();
		if(depth!=1) {//if depth is not 1, we can pre-sort our moves to have increased pruning speed and improve efficiency.
					  //we can also use the cutoff value which will only continue searching x best nodes. if depth is 1, we
					  //want to evaluate all nodes so we can prune, and no need to pre-sort as they will be evaluated here.
			useCutoff = true;
			legalMoves = calculateLegalMovesPriority(isOp, gameWeight);//pre-sorted legal moves
		}
		else {
			useCutoff = false;
			legalMoves = calculateLegalMoves(isOp);//unsorted legal moves
		}
		//System.out.println("Maximizer");
		
		while (!legalMoves.isEmpty()) {//while there are still legal moves to continue evaluation
			//System.out.println("NOT BREAKING WHILE MAX. CUTOFF" + cutoffCounter + "USECUTOFF" + useCutoff);
			if(useCutoff&&cutoffCounter>moveCheckCutoff) {//if the usecutoff variable is true, and we have already evaluated x best moves
				//System.out.println("BREAKING WHILE MAX");
				break;
			}
			if (startTime != -1) {//if using iterative deepening, starTime will not be -1
				if (System.currentTimeMillis()-startTime>timeout) {//checks if timeout has been elapsed
					System.out.println("TIMER DEAD MAX");
					return 0;
				}
			}
			cutoffCounter++;//increments cutoffcoutner for x best moves evaluation
			

			Action move = legalMoves.remove();//remove current move from queue

			makeMove(move);//make the move in the global gameboard
			int rating = minimizer((byte) (depth - 1), alpha, beta);//expand this move in minimizer

			undoMove(move);//undo the move in the global gameboard
			if (rating > alpha) {//set alpha
				alpha = rating;
				if (depth == super.searchDepth) {//if we are at the top node, set the bestMove
					bestMove = move;
				}
			}

			if (alpha >= beta) {//for pruning
				
				return alpha;
			}

		}


		return alpha;

	}
	public int minimizer(byte depth, int alpha, int beta) {
		if (depth == 0) {//if depth is 0 same as maximizer just evaluate position and return evaluation
			EvaluatePosition eval = new EvaluatePosition(completedGameMoves);
			int val = eval.evaluate(super.board, super.side, super.friendlyQueens, super.enemyQueens, this.gameWeight, (byte) 0);
			return(val);
			
		}
		boolean useCutoff = false;
		int cutoffCounter = 0;
		byte isOp = 1;

		Queue<Action> legalMoves = new LinkedList<>();
		if(depth!=1) {//same as maximizer
			useCutoff = true;
			legalMoves = calculateLegalMovesPriority(isOp, gameWeight);
		}
		else {
			useCutoff = false;
			legalMoves = calculateLegalMoves(isOp);
		}
		int counter = 0;
		while(!legalMoves.isEmpty()){
			if(useCutoff&&cutoffCounter>moveCheckCutoff) {
				//System.out.println("BREAKING WHILE MIN");
				break;
			}
			if (startTime != -1) {
				if (System.currentTimeMillis()-startTime>timeout) {
					System.out.println("TIMER DEAD MIN");
					return 0;
				}
			}
			cutoffCounter++;
			counter++;
			Action move = legalMoves.remove();

			makeMove(move);
			int rating = maximizer((byte) (depth-1), alpha, beta);

			undoMove(move);
			if (rating <= beta) {//set beta
				beta = rating;
			}


			if (alpha >= beta) {//for pruning

				
				return beta;
			}

		}


		return beta;
	}

}
