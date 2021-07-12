package ncl.poetsj.apps.dphilpn;

import java.util.Random;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilMessage;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilState;

public class DPhilDevice extends PDevice<DPhilState, Void, DPhilMessage> {

	public static final int maxTransConnects = 6;
	
	public static int localPlaces;
	public static int outPlaces = 4;
	public static int transitions;
	
	public static class Transition {
		public int type;
		public int[] p = new int[maxTransConnects];
		public Transition(int type, int... p) {
			this.type = type;
			for(int i=0; i<this.p.length && i<p.length; i++)
				this.p[i] = p[i];
		}
	}

	public static class OutMap {
		public int dev;
		public int place;
		public OutMap() {
		}
		public OutMap(int dev, int place) {
			this.dev = dev;
			this.place = place;
		}
	}

	public static Transition[] tmap;
	
	//public static int[] ttrackEnabled;
	//public static int[] ttrackNotFired;
	
	public static class DPhilMessage {
		public int dev;
		public int place;
		public int st;
	}

	public static class DPhilState {
		public int dev;
		public int time;
		public int timeLimit;
		public boolean live;
		public int countChanges;
		public long seed;
		
		public int[] places = new int[localPlaces+outPlaces];
		public boolean[] changed = new boolean[localPlaces+outPlaces];
		
		public OutMap[] outMap = new OutMap[outPlaces];
		public DPhilState() {
			for(int i=0; i<outMap.length; i++)
				outMap[i] = new OutMap();
		}
	}

	public static class DPhilGraph extends PGraph<DPhilDevice, DPhilState, Void, DPhilMessage> {
		@Override
		protected DPhilDevice createDevice() {
			return new DPhilDevice();
		}
	}

	@Override
	protected DPhilState createState() {
		return new DPhilState();
	}

	@Override
	protected DPhilMessage createMessage() {
		return new DPhilMessage();
	}

	@Override
	public void init() {
		s.time = 0;
		s.live = true;
		for(int p = 0; p<localPlaces+outPlaces; p++)
			s.changed[p] = false;
		s.countChanges = 0;
		readyToSend = NO;
	}

	@Override
	public void send(DPhilMessage msg) {
		readyToSend = NO;
		if(s.countChanges>0) {
			for(int p = localPlaces, op = 0; op<outPlaces; p++, op++) {
				if(s.changed[p]) {
					msg.dev = s.outMap[op].dev;
					msg.place = s.outMap[op].place;
					msg.st = 1; // s.placesUpd[p];
					//s.places[p] = 1; // s.placesUpd[p];
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
	public void recv(DPhilMessage msg, Void edge) {
		if(msg.dev==s.dev) {
			s.places[msg.place] = msg.st;
			s.live = true;
		}
	}
	
	public static final Random random = new Random();
	
	@Override
	public boolean step() {
		if(!s.live || s.time>=s.timeLimit)
			return false;
		if(logMessages)
			System.out.printf("%d:step\n", deviceId);
		
		/*boolean[] en = new boolean[transitions];
		for(int ti = 0; ti<transitions; ti++) {
			Transition t = tmap[ti];
			if(t.type==0)
				continue;
			int ins = t.type&0x0f;
			boolean enabled = true;
			for(int in=0; in<ins; in++) {
				if(s.places[t.p[in]]==0) {
					enabled = false;
					break;
				}
			}
			en[ti] = enabled;
			if(enabled)
				ttrackEnabled[ti]++;
		}*/
		
		int tCount = 0;
		//long rand = 0;
		//int sh = 0;
		
		int[] sh = new int[transitions];
		for(int i=0; i<transitions; i++)
			sh[i] = i;

		for(int ti = 0; ti<transitions; ti++) {
			/*if((ti&3)==0) {
				rand >>= 2;
				if(rand==0) {
					s.seed = (s.seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
					rand = s.seed>>4;
				}
				sh = (int)(rand&0x3);
				//sh = random.nextInt(4);
			}
			int idx = ti^sh;*/
			
			int x = random.nextInt(transitions-ti);
			int idx = sh[ti+x];
			sh[ti+x] = sh[ti];
			
			Transition t = tmap[idx];
			if(t.type==0)
				continue;
			
			int ins = t.type&0x0f;
			int outs = (t.type>>4)&0x0f;
			boolean enabled = true;
			/*for(int in=0; in<ins; in++) {
				if(s.places[t.p[in]]==0) {
					enabled = false;
					break;
				}
			}*/
			switch(ins) {
				case 1:
					if(s.places[t.p[0]]==0)
						enabled = false;
					break;
				case 2:
					if(s.places[t.p[0]]==0 || s.places[t.p[1]]==0)
						enabled = false;
					break;
				case 3:
					if(s.places[t.p[0]]==0 || s.places[t.p[1]]==0 || s.places[t.p[2]]==0)
						enabled = false;
					break;
			}
			
			if(enabled) {
				if(logMessages)
					System.out.printf("%d:t[%d] : ", deviceId, idx);
				/*int pi = 0;
				for(int in=0; in<ins; in++, pi++) {
					int p = t.p[pi];
					s.places[p] = 0;
					// s.changed[p] = true;
					// s.placesUpd[p] = 0;
					if(logMessages)
						System.out.printf("p[%d]=0; ", p);
				}
				for(int out=0; out<outs; out++, pi++) {
					int p = t.p[pi];
					s.changed[p] = true;
					// s.placesUpd[p] = 1;
					if(logMessages)
						System.out.printf("p[%d]=1; ", p);
				}*/
				switch(ins) {
					case 1:
						s.places[t.p[0]] = 0;
						break;
					case 2:
						s.places[t.p[0]] = 0;
						s.places[t.p[1]] = 0;
						break;
					case 3:
						s.places[t.p[0]] = 0;
						s.places[t.p[1]] = 0;
						s.places[t.p[2]] = 0;
						break;
				}
				switch(outs) {
					case 1:
						s.changed[t.p[ins+0]] = true;
						break;
					case 2:
						s.changed[t.p[ins+0]] = true;
						s.changed[t.p[ins+1]] = true;
						break;
					case 3:
						s.changed[t.p[ins+0]] = true;
						s.changed[t.p[ins+1]] = true;
						s.changed[t.p[ins+2]] = true;
						break;
				}
				
				//en[idx] = false;
				
				if(logMessages)
					System.out.println();
				
				tCount++;
				s.time++;
				if(s.time>=s.timeLimit)
					break;
			}
		}
		/*for(int ti = 0; ti<transitions; ti++) {
			if(en[ti]) ttrackNotFired[ti]++;
		}*/
		System.out.printf("tCount=%d\n", tCount);

		s.countChanges = 0;
		for(int p = 0; p<localPlaces+outPlaces; p++)
			if(s.changed[p]) s.countChanges++;
		s.live = false;
		if(logMessages)
			System.out.printf("%d:countChanges=%d\n", deviceId, s.countChanges);
		
		for(int p = 0; p<localPlaces; p++) {
			if(s.changed[p]) {
				s.places[p] = 1; // s.placesUpd[p];
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
	public boolean finish(DPhilMessage msg) {
		msg.dev = s.time;
		return true;
	}

}
