package ubc.cosc322;

import java.util.Comparator;
//this class is used for sorting oponent moves, as we need to sort them based on worst to best for us
public class OponentMoveComparator implements Comparator<Action> {

	@Override
	public int compare(Action o1, Action o2) {
		// TODO Auto-generated method stub
		return o1.v < o2.v ? -1 : 1;
	}

}
