package ncl.poetsj;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class EdgeList {

	public final ArrayList<ArrayList<Integer>> neighbours = new ArrayList<>();

	public int numNodes;
	public int maxFanout;
	
	public EdgeList read(String path) {
		try {
			Scanner in = new Scanner(new File(path));
			
			while(in.hasNext()) {
				String[] s = in.nextLine().split("\\s+", 2);
				int src = Integer.parseInt(s[0]);
				while(neighbours.size()<=src) {
					neighbours.add(new ArrayList<Integer>());
				}
				int dst = Integer.parseInt(s[1]);
				neighbours.get(src).add(dst);
			}
			numNodes = neighbours.size();
			maxFanout = 0;
			for(ArrayList<Integer> list : neighbours) {
				int s = list.size();
				if(s>maxFanout)
					maxFanout = s;
			}
			
			in.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return this;
	}

}
