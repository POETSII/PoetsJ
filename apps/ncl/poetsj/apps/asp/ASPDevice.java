package ncl.poetsj.apps.asp;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.asp.ASPDevice.ASPMessage;
import ncl.poetsj.apps.asp.ASPDevice.ASPState;

public class ASPDevice extends PDevice<ASPState, Void, ASPMessage> {

	public static final int NROOTS = 10;
	
	public static class ASPGraph extends PGraph<ASPDevice, ASPState, Void, ASPMessage> {
		@Override
		protected ASPDevice createDevice() {
			return new ASPDevice();
		}
	}
	
	public static class ASPState {
		public int globalId;
		public int rootIdx;
		
		public int[] buff = new int[NROOTS];
		public boolean[] updated = new boolean[NROOTS];
		public int updatePending;
	}
	
	public static class ASPMessage {
		public int rootIdx;
		public int hops;
	}

	@Override
	protected ASPState createState() {
		return new ASPState();
	}

	@Override
	protected ASPMessage createMessage() {
		return new ASPMessage();
	}

	@Override
	public void init() {
		for(int i = 0; i < NROOTS; i++) {
			s.buff[i] = 0x7fffffff;
			s.updated[i] = false;
		}
		s.updatePending = 0;
		if(s.rootIdx>=0)
			readyToSend = pin(0);
	}

	@Override
	public void send(ASPMessage msg) {
		if(s.rootIdx>=0) {
			s.buff[s.rootIdx] = 0;
			msg.rootIdx = s.rootIdx;
			msg.hops = 0;
			s.rootIdx = -1;
		}
		else {
			for(int i = 0; i < NROOTS; i++) {
				if(s.updated[i]) {
					msg.rootIdx = i;
					msg.hops = s.buff[i];

					s.updated[i] = false;
					s.updatePending--;
					break;
				}
			}
		}
		if(s.updatePending>0)
			readyToSend = pin(0);
		else
			readyToSend = NO;
	}

	@Override
	public void recv(ASPMessage msg, Void edge) {
		if(msg.rootIdx < NROOTS) {
			if(msg.hops+1 < s.buff[msg.rootIdx]) {
				s.buff[msg.rootIdx] = msg.hops+1;
				if(!s.updated[msg.rootIdx]) {
					s.updatePending++;
				}
				s.updated[msg.rootIdx] = true;
			}
		}
		if(s.updatePending>0)
			readyToSend = pin(0);
	}

	@Override
	public boolean step() {
		readyToSend = NO;
		return false;
	}

	@Override
	public boolean finish(ASPMessage msg) {
		int total = 0;
		for(int i = 0; i < NROOTS; i++) {
			total += s.buff[i];
		}
		msg.hops = total;
		msg.rootIdx = s.globalId;
		return true;
	}

}
