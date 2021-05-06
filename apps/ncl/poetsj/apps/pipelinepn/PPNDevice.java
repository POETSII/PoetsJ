package ncl.poetsj.apps.pipelinepn;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.pipelinepn.PPNDevice.PPNMessage;
import ncl.poetsj.apps.pipelinepn.PPNDevice.PPNState;

public class PPNDevice extends PDevice<PPNState, Void, PPNMessage> {

	public static final int MAX_PLACES = 1024;
	public static final int MAX_TRANSITIONS = 900;
	public static final int MAX_OUTMAP = 4;

	public static int localPlaces;
	public static int outPlaces = 4;
	public static int transitions;
	
	public static class Transition {
		int type;
		int p, p1, p2;
		public Transition(int type, int p, int p1, int p2) {
			this.type = type;
			this.p = p;
			this.p1 = p1;
			this.p2 = p2;
		}
		public Transition(int type, int p, int p1) {
			this(type, p, p1, 0);
		}
		public Transition(int type) {
			this(type, 0, 0, 0);
		}
	}

	public static class OutMap {
		int dev;
		int place;
	}

	public static Transition[] tmap;
	
	public static class PPNMessage {
		int dev;
		int place;
		int st;
		@Override
		public String toString() {
			return String.format("%d.p[%d]=%d", dev, place, st);
		}
	}

	public static class PPNState {
		int dev;
		int time;
		int timeLimit;
		boolean live;
		int countChanges;
		int watchPlace;
		
		int[] places = new int[MAX_PLACES];
		int[] placesUpd = new int[MAX_PLACES];
		boolean[] changed = new boolean[MAX_PLACES];
		
		OutMap[] outMap = new OutMap[MAX_OUTMAP];
		public PPNState() {
			for(int i=0; i<outMap.length; i++)
				outMap[i] = new OutMap();
		}
	}

	public static class PPNGraph extends PGraph<PPNDevice, PPNState, Void, PPNMessage> {
		@Override
		protected PPNDevice createDevice() {
			return new PPNDevice();
		}
	}

	@Override
	protected PPNState createState() {
		return new PPNState();
	}

	@Override
	protected PPNMessage createMessage() {
		return new PPNMessage();
	}

	@Override
	public void init() {
		s.time = 0;
		s.live = true;
		for(int p = 0; p<MAX_PLACES; p++)
			s.changed[p] = false;
		s.countChanges = 0;
		readyToSend = NO;
	}

	@Override
	public void send(PPNMessage msg) {
		readyToSend = NO;
		msg.dev = -1;
		if(s.countChanges>0) {
			for(int p = localPlaces, op = 0; op<outPlaces; p++, op++) {
				if(s.changed[p]) {
					msg.dev = s.outMap[op].dev;
					msg.place = s.outMap[op].place;
					msg.st = s.placesUpd[p];
					s.places[p] = s.placesUpd[p];
					s.changed[p] = false;
					s.countChanges--;
					break;
				}
			}
			if(s.countChanges>0)
				readyToSend = pin(0);
		}
	}

	@Override
	public void recv(PPNMessage msg, Void edge) {
		if(msg.dev==s.dev) {
			s.places[msg.place] = msg.st;
			s.live = true;
		}
	}
	
	private void updPlace(int p, int st) {
		s.changed[p] = true;// s.places[p]!=st; //st!=0; // TODO check?
		s.placesUpd[p] = st;
	}

	@Override
	public boolean step() {
		if(!s.live || s.time>=s.timeLimit)
			return false;
		if(logMessages)
			System.out.printf("%d:step\n", deviceId);
		
		for(int ti = 0; ti<MAX_TRANSITIONS; ti++) {
			Transition t = tmap[ti];
			if(t.type==1) {
				if(s.places[t.p]==0 && s.places[t.p1]==1) {
					updPlace(t.p, 1);
					updPlace(t.p1, 0);
				}
			}
			else if(t.type==2) {
				if(s.places[t.p]==0 && s.places[t.p1]==1 && s.places[t.p2]==1) {
					updPlace(t.p, 1);
					updPlace(t.p1, 0);
					updPlace(t.p2, 0);
				}
			}
			else if(t.type==3) {
				if(s.places[t.p]==1 && s.places[t.p1]==0 && s.places[t.p2]==0) {
					updPlace(t.p, 0);
					updPlace(t.p1, 1);
					updPlace(t.p2, 1);
				}
			}
			else {
				break;
			}
			s.time++;
			if(s.time>=s.timeLimit)
				break;
		}

		s.countChanges = 0;
		for(int p = 0; p<MAX_PLACES; p++)
			if(s.changed[p]) s.countChanges++;
		s.live = false;
		
		for(int p = 0; p<localPlaces; p++) {
			if(s.changed[p]) {
				s.places[p] = s.placesUpd[p];
				s.changed[p] = false;
				s.countChanges--;
				s.live = true;
			}
		}
		
		if(s.time>=s.timeLimit)
			return false;

		if(s.live || s.countChanges>0) {
			if(s.countChanges>0)
				readyToSend = pin(0);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean finish(PPNMessage msg) {
		msg.dev = s.time;
		msg.st = s.places[s.watchPlace];
		return true;
	}

}
