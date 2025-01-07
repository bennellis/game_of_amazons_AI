package ubc.cosc322;
//this class represents a legal game move, using the coordinates for the queen location to be moved,
//the coordinates for where to move that queen, and the coordinates for where it will shoot the arrow.
//it also has a value v which represents the evaluation of this game state (after making the move) for
//alpha beta pre-sorting improvements.
public class Action implements Comparable<Action>{
	public byte[] prev = new byte[2];
	public byte[] curr = new byte[2];
	public byte[] arrow = new byte[2];
	public int v;

	//if no v is used in constructor, initialize to 0
	public Action(byte[] prev, byte[] curr, byte[] arrow) {
		this.prev[0] = prev[0];
		this.prev[1] = prev[1];
		this.curr[0] = curr[0];
		this.curr[1] = curr[1];
		this.arrow[0] = arrow[0];
		this.arrow[1]=arrow[1];
		this.v = 0;
	}
	public Action(byte[] prev, byte[] curr, byte[] arrow, int v) {
		this.prev[0] = prev[0];
		this.prev[1] = prev[1];
		this.curr[0] = curr[0];
		this.curr[1] = curr[1];
		this.arrow[0] = arrow[0];
		this.arrow[1]=arrow[1];
		this.v = v;
	}
	//this is so we can sort based on the v values
	public int compareTo(Action o) {
		return o.v > this.v ? 1 : -1;
	}
	//this function is for determining if the opponents move is legal or not
	public boolean equals(Action A){
		if(!(A.arrow[0] == this.arrow[0]))
			return false;
		if(!(A.arrow[1] == this.arrow[1]))
			return false;
		if(!(A.curr[0] == this.curr[0]))
			return false;
		if(!(A.curr[1] == this.curr[1]))
			return false;
		if(!(A.prev[0] == this.prev[0]))
			return false;
		if(!(A.prev[1] == this.prev[1]))
			return false;
		return true;
	}
}
