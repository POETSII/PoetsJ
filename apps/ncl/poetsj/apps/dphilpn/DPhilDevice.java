package ncl.poetsj.apps.dphilpn;

import java.util.Random;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilMessage;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilState;

public class DPhilDevice extends PDevice<DPhilState, Void, DPhilMessage> {

	public static int localPlaces;
	public static int outPlaces = 4;
	public static int transitions;
	
	public static class Transition {
		int type;
		int[] p = new int[4];
		public Transition(int type, int... p) {
			this.type = type;
			for(int i=0; i<this.p.length && i<p.length; i++)
				this.p[i] = p[i];
		}
	}

	public static class OutMap {
		int dev;
		int place;
		public OutMap() {
		}
		public OutMap(int dev, int place) {
			this.dev = dev;
			this.place = place;
		}
	}

	public static Transition[] tmap;
	
	public static class DPhilMessage {
		int dev;
		int place;
		int st;
	}

	public static class DPhilState {
		int dev;
		int time;
		int timeLimit;
		boolean live;
		int countChanges;
		long seed;
		
		int[] places = new int[localPlaces+outPlaces];
		boolean[] changed = new boolean[localPlaces+outPlaces];
		
		OutMap[] outMap = new OutMap[outPlaces];
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
		
		long rand = 0;
		int sh = 0;
		for(int ti = 0; ti<transitions; ti++) {
			if((ti&3)==0) {
				rand >>= 2;
				if(rand==0) {
					s.seed = (s.seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
					rand = s.seed>>4;
				}
				sh = (int)(rand&0x3);
				//sh = random.nextInt(4);
			}
			Transition t = tmap[ti^sh];
			if(t.type==0)
				continue;
			
			int ins = t.type&0x0f;
			int outs = (t.type>>4)&0x0f;
			boolean enabled = true;
			for(int in=0; in<ins; in++) {
				if(s.places[t.p[in]]==0) {
					enabled = false;
					break;
				}
			}
			if(enabled) {
				if(logMessages)
					System.out.printf("%d:t[%d] : ", deviceId, ti^sh);
				int pi = 0;
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
				}
				if(logMessages)
					System.out.println();
				
				s.time++;
				if(s.time>=s.timeLimit)
					break;
			}
		}

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
