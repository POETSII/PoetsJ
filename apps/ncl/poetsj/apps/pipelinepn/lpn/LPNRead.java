package ncl.poetsj.apps.pipelinepn.lpn;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class LPNRead {

	public static class OutMap {
		public int dev;
		public Place place;
	}
	
	public static class Place implements Comparable<Place> {
		public int index;
		public String name;
		public boolean marked = false;
		public boolean choice = false;
		public OutMap out = null;
		@Override
		public String toString() {
			String s = String.format("P[%d] %s%s", index, marked ? "*" : "", name);
			if(out!=null)
				s += String.format(" (out%+d:%s)", out.dev, out.place.name);
			return s;
		}
		@Override
		public int compareTo(Place o) {
			int res = Boolean.compare(this.out!=null, o.out!=null);
			if(res==0)
				res = compareName(this.name, o.name);
			return res;
		}
	}

	public static class Transition implements Comparable<Transition> {
		public String name;
		public ArrayList<Place> in = new ArrayList<>();
		public ArrayList<Place> out = new ArrayList<>();
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("T ");
			sb.append(name);
			sb.append(" in:{");
			for(Place p : in) {
				sb.append(p.name);
				sb.append(";");
			}
			sb.append("} out:{");
			for(Place p : out) {
				sb.append(p.name);
				sb.append(";");
			}
			sb.append("}");
			return sb.toString();
		}
		@Override
		public int compareTo(Transition o) {
			return compareName(this.name, o.name);
		}
	}

	public HashMap<String, Place> placeMap = new HashMap<>();
	public HashMap<String, Transition> transitionMap = new HashMap<>();

	public ArrayList<Place> places;
	public ArrayList<Transition> transitions;
	public int localPlaces;
	public int outPlaces;
	
	public static int getNamePrefix(String n) {
		return n.startsWith("ccw_") ? -1 : n.startsWith("cw_") ? 1 : 0;
	}
	
	public static int compareName(String n1, String n2) {
		int res = Integer.compare(getNamePrefix(n1), getNamePrefix(n2));
		if(res==0)
			n1.compareTo(n2);
		return res;
	}
	
	public Place getPlaceByName(String name) {
		Place p = placeMap.get(name);
		if(p==null) {
			p = new Place();
			p.index = placeMap.size();
			p.name = name;
			placeMap.put(name, p);
		}
		return p;
	}
	
	public LPNRead load(String path) {
		try {
			Scanner in = new Scanner(new FileInputStream(new File(path)));
			
			int[] reorder = null;
			
			while(in.hasNext()) {
				String line = in.nextLine();
				if(line.isEmpty() || line.startsWith("#"))
					continue;
				String[] s = line.split("\\s+");
				for(int i=0; i<s.length; i++) {
					if(s[i].startsWith("{"))
						s[i] = s[i].substring(1);
					if(s[i].endsWith("}"))
						s[i] = s[i].substring(0, s[i].length()-1);
				}
				if(s[0].startsWith(".")) {
					if(s[0].equals(".dummy")) {
						for(int i=1; i<s.length; i++) {
							Transition t = new Transition();
							t.name = s[i];
							transitionMap.put(t.name, t);
						}
					}
					else if(s[0].equals(".marking")) {
						for(int i=1; i<s.length; i++) {
							Place p = placeMap.get(s[i]);
							p.marked = true;
						}
					}
					else if(s[0].equals(".out")) {
						Place p = placeMap.get(s[1]);
						p.out = new OutMap();
						
						if(s[2].equals("ccw"))
							p.out.dev = -1;
						else if(s[2].equals("cw"))
							p.out.dev = 1;
						else
							throw new RuntimeException();
						p.out.place = placeMap.get(s[3]);
					}
					else if(s[0].equals(".reorder")) {
						reorder = new int[s.length-1];
						for(int i=1; i<s.length; i++) {
							reorder[i-1] = Integer.parseInt(s[i]);
						}
					}
				}
				else {
					Transition t = transitionMap.get(s[0]);
					if(t!=null) {
						for(int i=1; i<s.length; i++)
							t.out.add(getPlaceByName(s[i]));
					}
					else {
						Place p = getPlaceByName(s[0]);
						for(int i=1; i<s.length; i++)
							transitionMap.get(s[i]).in.add(p);
						if(s.length>2)
							p.choice = true;
					}
				}
			}
			
			places = new ArrayList<>(placeMap.values());
			places.sort(null);
			localPlaces = 0;
			outPlaces = 0;
			int index = 0;
			for(Place p : places) {
				if(p.out==null)
					localPlaces++;
				else
					outPlaces++;
				p.index = index++;
			}
			
			transitions = new ArrayList<>(transitionMap.values());
			transitions.sort(null);
			if(reorder!=null) {
				ArrayList<Transition> treorder = new ArrayList<>(transitions.size());
				for(int i=0; i<transitions.size(); i++)
					treorder.add(transitions.get(reorder[i]));
				transitions = treorder;
			}
			
			in.close();
			return this;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void print() {
		for(Place p : places) {
			System.out.println(p);
		}
		for(Transition t : transitions)
			System.out.println(t);
	}

}
