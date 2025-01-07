package ubc.cosc322;
import java.util.*;
public class Player {
	public static final byte[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, };

	public byte[][] board = new byte[10][10];
	public byte side; //side = 0 means we are black
					  //side = 1 means we are white
	public byte searchDepth;
	public byte[] bestMove;
	public byte[][] friendlyQueens;
	public byte[][] enemyQueens;
	int debugcount = 10;
	public byte completedGameMoves;

	
	
	
	public Player(byte[][] board, byte side, byte searchDepth, byte[][] friendlyQueens, byte[][] enemyQueens, byte completedGameMoves) {
		this.board = board;
		this.side = side;
		this.searchDepth = searchDepth;
		this.friendlyQueens = friendlyQueens;
		this.enemyQueens = enemyQueens;
		this.completedGameMoves = completedGameMoves;
		
	}
	
	//computes legal moves without priority sorting, isOp is 1 for opponent, 0 for my moves
	public Queue<Action> calculateLegalMoves(byte isOp) {
		Queue<Action> legalMoves = new LinkedList<Action>();
		byte[][] tempQueens = new byte[4][2];
		if(isOp==0) {//sets temp queens to be friendly or enemy based on isOp value
			tempQueens[0] = friendlyQueens[0];
			tempQueens[1] = friendlyQueens[1];
			tempQueens[2] = friendlyQueens[2];
			tempQueens[3] = friendlyQueens[3];
		}
		else if(isOp==1) {
			tempQueens[0] = enemyQueens[0];
			tempQueens[1] = enemyQueens[1];
			tempQueens[2] = enemyQueens[2];
			tempQueens[3] = enemyQueens[3];
		}
		
		for(byte[] Queen : tempQueens) {//for each queen
			//System.out.println("old queens:" + Queen[0] + " " + Queen[1]);
			for (byte[] direction : directions) {//for each direction
				byte c = (byte) (Queen[0] + direction[0]);
				byte r = (byte) (Queen[1] + direction[1]);
				
				//while c&r are in bounds and is free
				while(c < 10 && c >=0 && r < 10 && r >=0 && board[c][r] == 0) {
					byte[] oldtempQueen = new byte[2];
					oldtempQueen[0] = Queen[0];
					oldtempQueen[1] = Queen[1];
					byte[] newQueen = new byte[2];
					newQueen[0] = c;
					newQueen[1] = r;
					//for every possible queen move, it will calculate every possible arrow that new queen could fire
					Queue<byte[]> legalArrows = computeLegalArrows(oldtempQueen, newQueen);
					while(!legalArrows.isEmpty()) {//for every arrow that new queen could fire
						
						//adds the old queen, new queen, and legal arrow to an action and adds it to legal moves queue.
						Action A = new Action(oldtempQueen, newQueen, legalArrows.remove());
						legalMoves.add(A);
						//System.out.println("adding move with old pos: " + oldtempQueen[0] + " " + oldtempQueen[1]);
					}
					c+=direction[0];//hops one more time if within gameboard and empty in the same direction
					r+=direction[1];
				}
			}
		}
		return legalMoves;
	}
	
	//computes legal moves with priority, only difference from above is it adds it to a priority queue rather 
	//than a normal queue. gameweight is the w of the real game board.
	public Queue<Action> calculateLegalMovesPriority(byte isOp, double gameWeight) {
		Queue<Action> legalMoves = new PriorityQueue<>();
		byte[][] tempQueens = new byte[4][2];
		if(isOp==0) {
			tempQueens[0] = friendlyQueens[0];
			tempQueens[1] = friendlyQueens[1];
			tempQueens[2] = friendlyQueens[2];
			tempQueens[3] = friendlyQueens[3];
		}
		else{
			legalMoves = new PriorityQueue<>(new OponentMoveComparator());//if isOp = 1, changes the comparator to have
																		  //worst moves at front of queue.
			tempQueens[0] = enemyQueens[0];
			tempQueens[1] = enemyQueens[1];
			tempQueens[2] = enemyQueens[2];
			tempQueens[3] = enemyQueens[3];
		}
		
		
		for(byte[] Queen : tempQueens) {
			//System.out.println("old queens:" + Queen[0] + " " + Queen[1]);
			for (byte[] direction : directions) {
				byte c = (byte) (Queen[0] + direction[0]);
				byte r = (byte) (Queen[1] + direction[1]);
				
				//while c&r are in bounds and is free
				while(c < 10 && c >=0 && r < 10 && r >=0 && board[c][r] == 0) {
					byte[] oldtempQueen = new byte[2];
					oldtempQueen[0] = Queen[0];
					oldtempQueen[1] = Queen[1];
					byte[] newQueen = new byte[2];
					newQueen[0] = c;
					newQueen[1] = r;
					Queue<byte[]> legalArrows = computeLegalArrows(oldtempQueen, newQueen);
					
					//everything above this point is the same as the normal legalmoves function.
					
					while(!legalArrows.isEmpty()) {//for every legal arrow
						byte[] newArrow = legalArrows.remove();//removes the arrow
						int v = 0; //partial evaluation
						Action A = new Action(oldtempQueen, newQueen, newArrow);//creates the action
						makeMove(A);//makes the move in the global game board
						EvaluatePosition eval = new EvaluatePosition(completedGameMoves);//evaluates the gameboard
						v = (eval.evaluate(board, side, friendlyQueens, enemyQueens, gameWeight, isOp));
						undoMove(A);//undoes the Action A
						Action B = new Action(oldtempQueen, newQueen, newArrow, v);
						//Action B is the same as A but with the evaluation v.
						
						legalMoves.add(B);//adds B to the priority queue
						//System.out.println("adding move with old pos: " + oldtempQueen[0] + " " + oldtempQueen[1]);
					}
					c+=direction[0];
					r+=direction[1];
				}
			}
		}
		return legalMoves;
	}

	//this function calculates all legal arrows a queen can shoot, and also takes its old location so we can shoot there too.
	public Queue<byte[]> computeLegalArrows(byte[] oldQueen, byte[] newQueen) {
		Queue<byte[]> legalArrows= new LinkedList<byte[]>();
		for (byte[] direction : directions) {//for every direction
			byte c = (byte) (newQueen[0] + direction[0]);
			byte r = (byte) (newQueen[1] + direction[1]);

			// while c&r are in bounds and (is free or was the old position of the queen)
			while (c < 10 && c >= 0 && r < 10 && r >= 0 && (board[c][r] == 0 || (c==oldQueen[0] && r==oldQueen[1]))) {
				//System.out.println("Inside legal arrows while");
				byte[] Arrow = new byte[2];
				Arrow[0] = c;
				Arrow[1] = r;
				legalArrows.add(Arrow);
				c+=direction[0];
				r+=direction[1];
			}
		}
		return legalArrows;
	}
	//this function performs a move in the global game board based on Action A
	public void makeMove(Action A) {
		
		//get value of queen being moved
		byte movingQueen = board[A.prev[0]][A.prev[1]];
		//System.out.println(side);
		
		//System.out.println(movingQueen);
//		if(debugcount < 10) {
//			displayBoard();
//			debugcount++;
//		}
		//update game board
		board[A.prev[0]][A.prev[1]] = 0;//sets where queen used to be to free
		board[A.curr[0]][A.curr[1]] = movingQueen;//sets the new spot to be the value of the moved queen
		board[A.arrow[0]][A.arrow[1]] = 3;//sets the arrow location to be an arrow (3)
		
		if(debugcount < 10) {
			displayBoard();
			debugcount++;
		}
		//update friendly/enemy queen array
		boolean flag = true;
		if(movingQueen-1 == this.side) {
			
			for(int i = 0; i < 4; i++) {
				if(enemyQueens[i][0] == A.prev[0] && enemyQueens[i][1] == A.prev[1]) {
					enemyQueens[i][0] = A.curr[0];
					enemyQueens[i][1] = A.curr[1];
					flag = false;
					break;
				}
				
			}
			if(flag)
				System.out.println("ERROR IN MAKEMOVE ENEMY");
		}
		else if(2-movingQueen == this.side) {
			flag = true;
			for(int i = 0; i < 4; i++) {
				if(friendlyQueens[i][0] == A.prev[0] && friendlyQueens[i][1] == A.prev[1]) {
//					if(A.prev[0] == 3 && A.prev[0] == 0 && debugcount==10)
//						debugcount=0;
					//System.out.println("Moving queen from: (" + friendlyQueens[i][0] + " " + friendlyQueens[i][1] + "), to: (" + A.curr[0] + " " + A.curr[1] + ")");
					friendlyQueens[i][0] = A.curr[0];
					friendlyQueens[i][1] = A.curr[1];
					flag = false;
					break;
				}
				
			}
			if(flag)
				System.out.println("ERROR IN MAKEMOVE FRIENDLY");
		}
		else {
			System.out.println("ERROR IN MAKEMOVE");
		}
	
	}
	//this function checks if an opponents move is valid
	public boolean wasValid(Action A) { //Assumes we're only checking if the opponent made a legal move after the fact
		Queue<Action> OpponentMoves = calculateLegalMoves((byte) 1);
		while(!OpponentMoves.isEmpty()){
			Action move = OpponentMoves.remove();
			if(move.equals(A)){
				return true;
			}
		}
		return false;
	}
	//this function undoes a move in the global gameboard based on action A
	public void undoMove(Action A) {
		
		//get value of queen being unmoved
		byte movingQueen = board[A.curr[0]][A.curr[1]];
		//System.out.println("undo move");
		//System.out.println(side);
		//System.out.println(movingQueen);
		if(debugcount < 10) {
			displayBoard();
			System.out.println("moving queen = " + movingQueen);
			System.out.println("Moving queen from: (" + A.curr[0] + " " + A.curr[1] + "), to: (" + A.prev[0] + " " + A.prev[1] + ")");
			debugcount++;
		}
		
		//un-update game board (Make sure to update the arrow to 0 before updating the queen position!!!! 
		//in cases where arrow lands on where the queen used to be, if done otherwise will overwrite queen value to 0
		//Took way to long to debug...... :(
		board[A.arrow[0]][A.arrow[1]] = 0;
		board[A.prev[0]][A.prev[1]] = movingQueen;
		board[A.curr[0]][A.curr[1]] = 0;
		
		
		if(debugcount < 10) {
			displayBoard();
			debugcount++;
		}
		//update friendly/enemy queen array
		boolean flag = true;
		if(movingQueen-1 == this.side) {
			for(int i = 0; i < 4; i++) {
				if(enemyQueens[i][0] == A.curr[0] && enemyQueens[i][1] == A.curr[1]) {
					enemyQueens[i][0] = A.prev[0];
					enemyQueens[i][1] = A.prev[1];
					flag = false;
					break;
				}
				
			}
			if(flag)
				System.out.println("ERROR IN undoMOVE ENEMY");
		}
		else if(2-movingQueen == this.side) {
			flag = true;
			for(int i = 0; i < 4; i++) {
				if(friendlyQueens[i][0] == A.curr[0] && friendlyQueens[i][1] == A.curr[1]) {
					//System.out.println("Moving queen from: (" + friendlyQueens[i][0] + " " + friendlyQueens[i][1] + "), to: (" + A.prev[0] + " " + A.prev[1] + ")");
					
					friendlyQueens[i][0] = A.prev[0];
					friendlyQueens[i][1] = A.prev[1];
					flag = false;
					break;
				}
				
			}
			if(flag)
				System.out.println("ERROR IN undoMOVE FRIENDLY");
		}
		else {
			System.out.println("ERROR IN undoMOVE");
		}
	}
//this function prints friendly and enemy queens.
	public void displayQueens() {
		System.out.println("Friendly queens:");
		for (int i = 0; i < 4; i++) {
			System.out.print("(" + this.friendlyQueens[i][0] + ", " + this.friendlyQueens[i][1] + "), ");
		}
		System.out.println();
		System.out.println("Enemy queens:");
		for (int i = 0; i < 4; i++) {
			System.out.print("(" + this.enemyQueens[i][0] + ", " + this.enemyQueens[i][1] + "), ");
		}
		System.out.println();
	}
	
	public void displayBoard() {
		for(int i = 0; i < 10; i++) {
			System.out.println();
			for(int j = 0; j < 10; j++) {
				System.out.print(board[i][j] + " ");
			}
		}
		System.out.println();
	}


		
	
	
}
