package ncl.poetsj.apps.pipelinepn.lpn;

import static java.lang.System.out;

import ncl.poetsj.apps.pipelinepn.lpn.LPNRead.Place;
import ncl.poetsj.apps.pipelinepn.lpn.LPNRead.Transition;

public class LPNSave {

	public static void printLpn(LPNRead lpn, int steps) {
		out.println(".model Untitled");
		
		out.print(".dummy");
		for(int s=0; s<steps; s++) {
			for(Transition t : lpn.transitions)
				out.printf(" s%d_%s", s, t.name);
		}
		out.println();
		
		out.println(".graph");
		for(int s=0; s<steps; s++) {
			for(Transition t : lpn.transitions) {
				out.printf("s%d_%s", s, t.name);
				for(Place p : t.out) {
					if(p.out!=null) {
						if(p.out.dev<0)
							out.printf(" s%d_%s", s>0 ? s-1 : steps-1, p.out.place.name);
						else if(p.out.dev>0)
							out.printf(" s%d_%s", s<steps-1 ? s+1 : 0, p.out.place.name);
					}
					else
						out.printf(" s%d_%s", s, p.name);
				}
				out.println();
			}
			for(Place p : lpn.places) {
				if(p.out!=null)
					continue;
				out.printf("s%d_%s", s, p.name);
				for(Transition t : p.tout)
					out.printf(" s%d_%s", s, t.name);
				out.println();
			}
		}
		
		out.print(".marking {");
		boolean first = true;
		for(int s=0; s<steps; s++) {
			for(Place p : lpn.places) {
				if(p.marked) {
					if(!first) out.print(" ");
					first = false;
					out.printf("s%d_%s", s, p.name);
				}
			}
		}
		out.println("}");
		
		out.println(".end");
	}
	
	public static void main(String[] args) {
		printLpn(new LPNRead().load(LpnToPpn.path), 3);
	}

}
