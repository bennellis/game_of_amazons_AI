package ubc.cosc322;
//import java.util.*;
//import java.util.TimerTask;


public class IterativeDeepening extends AlphaBetaSearch {
	public Action iterativeBestMove = new Action(friendlyQueens[0], friendlyQueens[0], friendlyQueens[0]);
	public long timeout = 29000; //time for iterative deepening to run
    public IterativeDeepening(byte[][] board, byte side, byte searchDepth, byte[][] friendlyQueens, byte[][] enemyQueens, byte completedGameMoves){
        super(board, side, searchDepth,friendlyQueens,enemyQueens, completedGameMoves);
    }
    /*
     * For each possibile move, compute value
     */
    //@SuppressWarnings("static-access")
	public Action iterate(){
    	long startTime = System.currentTimeMillis();//initializes start time

        // Defines depth to be incremented:
        super.searchDepth = 1;
        

        while(System.currentTimeMillis()-startTime<timeout){//while it has not exceeded timeout, try to go another depth
        	System.out.println("Evaluating depth " + super.searchDepth);//print for user
        	Action BestMove = decideMove(startTime, timeout);//calls alpha beta with startime and timeout
        	//this if statement determines if the BestMove was returned before the timeout, and if we can use the value.
        	//if it was returned after the timeout, we can be almost certain that the value cannot be used as it was 
        	//interrupted during the search.
        	if(System.currentTimeMillis()-startTime<timeout) {
        		
//        		System.out.println("Finished depth, best move:");
//        		System.out.println("oldQueen: " + BestMove.prev[0] + ", " + BestMove.prev[1]);
//        		System.out.println("newQueen: " + BestMove.curr[0] + ", " + BestMove.curr[1]);
//        		System.out.println("Arrow: " + BestMove.arrow[0] + ", " + BestMove.arrow[1]);

        		this.iterativeBestMove = BestMove;//if the search finished without being interrupted, set the global best move
        	}
        	
        	//this is used at the end of the game when it is able to search to the end of the search tree, it will return before
        	//the time limit is up.
        	if(super.searchDepth + super.completedGameMoves > 100) {
        											
        		System.out.println("Exceeded max depth limit (no more moves available)");
        		break;
        	}
            //this.iterativeBestMove = decideMove(t);
            super.searchDepth++;
        }
        


        return iterativeBestMove; 
    }
}
