package ncl.poetsj.apps.dphilintpn;

import java.util.Random;

import ncl.poetsj.PGraph;
import ncl.poetsj.apps.dphilpn.DPhilDevice;

public class DPhilIntDevice extends DPhilDevice {

	//public static int[] ttrackEnabled;
	//public static int[] ttrackNotFired;
	
	public static class DPhilIntGraph extends PGraph<DPhilIntDevice, DPhilState, Void, DPhilMessage> {
		@Override
		protected DPhilIntDevice createDevice() {
			return new DPhilIntDevice();
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


	public static final Random random = new Random();
	
	@Override
	public boolean step() {
		if(!s.live || s.time>=s.timeLimit)
			return false;
		if(logMessages)
			System.out.printf("%d:step\n", deviceId);
		
		int tCount = 0;
		
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
			
			s.seed = (s.seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			int x = (int)((s.seed>>16L) & 0x7fffffff) % (transitions-ti); // random.nextInt(transitions-ti);
			int idx = sh[ti+x];
			sh[ti+x] = sh[ti];
			
			Transition t = tmap[idx];
			if(t.type==0)
				continue;
			
			int ins = t.type&0x0f;
			int outs = (t.type>>4)&0x0f;
			boolean enabled = true;
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
				
				if(logMessages)
					System.out.println();
				
				tCount++;
				s.time++;
				break;
			}
		}
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

}
