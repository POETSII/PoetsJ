package ncl.poetsj.apps.pipelinepn.lpn;

import static java.lang.System.out;

import ncl.poetsj.apps.pipelinepn.lpn.LPNRead.Place;
import ncl.poetsj.apps.pipelinepn.lpn.LPNRead.Transition;

public class LpnToPpn {

	public static final String path = "../Poets.Temp/pipeline_tile.lpn";
	//public static final String path = "../Poets.Temp/dphil_tile.pn.lpn";
	
	public static int transitionAlign = 1;
	
	public static void printTmapJava(LPNRead lpn, int steps) {
		out.println("// Places:");
		for(Place p : lpn.places) {
			out.printf("// %d%s: %s%s\n", p.index, p.marked ? "*" : "", p.name, p.out==null ? "" : " (out)");
		}
		
		out.printf("localPlaces = %d;\noutPlaces = %d;\n\n", lpn.localPlaces*steps, lpn.outPlaces);

		out.printf("// Initial marking: \ninitPlaces = new int[] {");
		for(int pi=0; pi<lpn.localPlaces; pi++) {
			if(pi>0) out.print(", ");
			out.print(lpn.places.get(pi).marked ? 1 : 0);
		}
		out.printf("};\ntileInitPlaces = %d;\n", lpn.localPlaces);
		
		out.printf("\n// Output mapping:\nint offs = 0;\ntOutMap = new OutMap[outPlaces];\n");
		for(int pi=0; pi<lpn.outPlaces; pi++) {
			Place p = lpn.places.get(pi+lpn.localPlaces);
			out.printf("tOutMap[offs++] = new OutMap(%d, %d);\n", p.out.dev, p.out.dev<0 ? lpn.localPlaces*(steps-1) + p.out.place.index : p.out.place.index);
		}

		int tsize = lpn.transitions.size();
		int transitions = ((tsize+(transitionAlign-1)) / transitionAlign) * transitionAlign;
		out.printf("\n// Transitions:\noffs = 0;\ntransitions = %d; // (%d aligned to %d) x %d steps\n"+
				"tmap = new Transition[transitions];\n\n// {0xoi, inp-s, outp-s}\n",
				transitions*steps, lpn.transitions.size(), transitionAlign, steps);
		
		for(int s=0; s<steps; s++) {
			for(int ti=0; ti<transitions; ti++) {
				if(ti>tsize) {
					out.println("tmap[offs++] = new Transition(0);");
				}
				else {
					int p0 = lpn.localPlaces*s;
					Transition t = lpn.transitions.get(ti);
					out.printf("tmap[offs++] = new Transition(0x%d%d", t.out.size(), t.in.size());
					boolean choice = false;
					for(Place p : t.in) {
						out.printf(", %d", p0 + p.index);
						if(p.choice)
							choice = true;
					}
					for(Place p : t.out) {
						if(p.out!=null) {
							if(p.out.dev<0 && s>0)
								out.printf(", %d /* prev */ ", p0 - lpn.localPlaces + p.out.place.index);
							else if(p.out.dev>0 && s<steps-1)
								out.printf(", %d /* next */ ", p0 + lpn.localPlaces + p.out.place.index);
							else
								out.printf(", %d /* out */ ", lpn.localPlaces*(steps-1) + p.index);
						}
						else {
							out.printf(", %d", p0 + p.index);
						}
					}
					out.printf("); // %d: %s%s\n", ti, t.name, choice ? " *choice" : "");
				}
				if(transitionAlign>1 && ti%transitionAlign==transitionAlign-1)
					out.println();
			}
		}
	}

	public static void printTmapGenC(String prefix, LPNRead lpn) {
		int tsize = lpn.transitions.size();
		int transitions = ((tsize+(transitionAlign-1)) / transitionAlign) * transitionAlign;
		out.printf("#define TMAP_NAME \"%s\"\n\n", prefix);
		out.printf("#if (MAX_TRANS_CONNECTS<%d)\n#error \"not enough connectivity in Transition\"\n#endif\n\n", getMaxConnects(lpn));
		
		out.printf("const uint16_t tileInitPlaces = %d;\n", lpn.localPlaces);
		out.printf("const uint8_t initPlaces[tileInitPlaces] = {");
		for(int pi=0; pi<lpn.localPlaces; pi++) {
			if(pi>0) out.print(", ");
			out.print(lpn.places.get(pi).marked ? 1 : 0);
		}
		out.println("};\n");

		out.printf("void generateTMap(uint32_t steps) {\n"+
				"\tnumPlaces = steps*%d;\n"+
				"\ttransitions = steps*%d;\n"+
				"\ttmap = new Transition[transitions];\n\n"+
				"\tuint32_t offs = 0;\n\tuint32_t p = 0;\n"+
				"\tfor(uint32_t s = 0; s<steps; s++) {\n", lpn.localPlaces, transitions);
		
		for(int ti=0; ti<transitions; ti++) {
			if(ti>tsize) {
				out.println("{0},");
			}
			else {
				//int p0 = lpn.localPlaces*s;
				Transition t = lpn.transitions.get(ti);
				out.printf("\t\ttmap[offs++] = {0x%d%d", t.out.size(), t.in.size());
				boolean choice = false;
				for(Place p : t.in) {
					out.printf(", p+%d", p.index);
					if(p.choice)
						choice = true;
				}
				for(Place p : t.out) {
					if(p.out!=null) {
						if(p.out.dev<0)
							out.printf(", (s>0 ? p-%d : (steps-1)*%d) + %d", lpn.localPlaces, lpn.localPlaces, p.out.place.index);
						else if(p.out.dev>0)
							out.printf(", (s<steps-1 ? p+%d : 0) + %d", lpn.localPlaces, p.out.place.index);
					}
					else {
						out.printf(", p+%d", p.index);
					}
				}
				out.printf("}; // %d: %s%s\n", ti, t.name, choice ? " *choice" : "");
				if(transitionAlign>1 && ti%transitionAlign==transitionAlign-1)
					out.println();
			}
		}
		
		out.printf("\t\tp += %d;\n\t}\n}", lpn.localPlaces);
	}

	public static int getMaxConnects(LPNRead lpn) {
		int max = 0;
		for(int ti=0; ti<lpn.transitions.size(); ti++) {
			Transition t = lpn.transitions.get(ti);
			int c = t.in.size() + t.out.size();
			if(c>max) max = c;
		}
		return max;
	}
	
	public static void printTmapC(String prefix, LPNRead lpn, int steps) {
		out.printf("#ifndef _%s_TMAP_H_\n#define _%s_TMAP_H_\n\n", prefix, prefix);
		out.printf("#define TMAP_NAME \"%s\"\n\n", prefix);
		out.printf("const uint16_t localPlaces = %d;\nconst uint8_t outPlaces = %d;\n", lpn.localPlaces*steps, lpn.outPlaces);
		
		int tsize = lpn.transitions.size();
		int transitions = ((tsize+(transitionAlign-1)) / transitionAlign) * transitionAlign;
		out.printf("const uint32_t transitions = %d; // (%d aligned to %d) x %d steps\n\n"+
				"#if (MAX_TRANS_CONNECTS<%d)\n#error \"not enough connectivity in Transition\"\n#endif\n\n"+
				"const Transition tmap[transitions] = {\n\t// {0xoi, inp-s, outp-s}\n",
				transitions*steps, lpn.transitions.size(), transitionAlign, steps, getMaxConnects(lpn));
		
		for(int s=0; s<steps; s++) {
			for(int ti=0; ti<transitions; ti++) {
				String comma = (s==steps-1 && ti==transitions-1) ? "" : ",";
				if(ti>tsize) {
					out.println("{0},");
				}
				else {
					int p0 = lpn.localPlaces*s;
					Transition t = lpn.transitions.get(ti);
					out.printf("\t{0x%d%d", t.out.size(), t.in.size());
					boolean choice = false;
					for(Place p : t.in) {
						out.printf(", %d", p0 + p.index);
						if(p.choice)
							choice = true;
					}
					for(Place p : t.out) {
						if(p.out!=null) {
							if(p.out.dev<0 && s>0)
								out.printf(", %d /* prev */ ", p0 - lpn.localPlaces + p.out.place.index);
							else if(p.out.dev>0 && s<steps-1)
								out.printf(", %d /* next */ ", p0 + lpn.localPlaces + p.out.place.index);
							else
								out.printf(", %d /* out */ ", lpn.localPlaces*(steps-1) + p.index);
						}
						else {
							out.printf(", %d", p0 + p.index);
						}
					}
					out.printf("}%s // %d:%d: %s%s\n", comma, s, ti, t.name, choice ? " *choice" : "");
				}
				if(transitionAlign>1 && ti%transitionAlign==transitionAlign-1)
					out.println();
			}
		}
		out.println("};\n\n#endif");
	}
	
	public static void printPInitC(String prefix, LPNRead lpn, int steps) {
		out.printf("#ifndef _%s_PINIT_H_\n#define _%s_PINIT_H_\n\n", prefix, prefix);
		out.printf("#define PINIT_NAME \"%s\"\n\n", prefix);
		out.println("// Places:");
		for(Place p : lpn.places) {
			out.printf("// %d%s: %s%s\n", p.index, p.marked ? "*" : "", p.name, p.out==null ? "" : " (out)");
		}
		
		out.printf("\n// Initial marking: \nconst uint8_t initPlaces[localPlaces] = {");
		for(int pi=0; pi<lpn.localPlaces; pi++) {
			if(pi>0) out.print(", ");
			out.print(lpn.places.get(pi).marked ? 1 : 0);
		}
		out.printf("};\nconst uint16_t tileInitPlaces = %d;\n", lpn.localPlaces);
		
		out.printf("\n// Output mapping:\nconst OutMap tOutMap[outPlaces] = {\n");
		for(int pi=0; pi<lpn.outPlaces; pi++) {
			String comma = (pi==lpn.outPlaces-1) ? "" : ",";
			Place p = lpn.places.get(pi+lpn.localPlaces);
			out.printf("\t{%d, %d}%s // %s to %s\n",
					p.out.dev, p.out.dev<0 ? lpn.localPlaces*(steps-1) + p.out.place.index : p.out.place.index,
					comma, p.name, p.out.place.name);
		}
		out.println("};\n\n#endif");
	}

	public static void main(String[] args) {
		//printTmapJava(new LPNRead().load(path), 2);
		printTmapGenC("PIPE", new LPNRead().load(path));
		//printTmapC("PIPE", new LPNRead().load(path), 46);
		//printPInitC("PIPE", new LPNRead().load(path), 46);
	}
	
}
