package ncl.poetsj.apps.qseismic;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.qseismic.QSeismicDevice.QSeismicMessage;
import ncl.poetsj.apps.qseismic.QSeismicDevice.QSeismicState;

public class QSeismicDevice extends PDevice<QSeismicState, Void, QSeismicMessage> {

	public static final int R = 5;
	public static final int D = R*2+1;
	public static final int MID = R*D+R;

	public static int makeIndex(int i, int j) {
		return i*D+j;
	}
	
	public static class QSeismicGraph extends PGraph<QSeismicDevice, QSeismicState, Void, QSeismicMessage> {
		@Override
		protected QSeismicDevice createDevice() {
			return new QSeismicDevice();
		}
	}
	
	public static class QSeismicState {
		public int x, y;
		public boolean isSource;
		public int[] w = new int[D*D];
		public int pending;
		public boolean[] changed = new boolean[D*D];
		public int[] dist = new int[D*D];
		public int parentx, parenty;
	}

	public static class QSeismicMessage {
		public int x, y;
		public int dist;
	}

	@Override
	protected QSeismicState createState() {
		return new QSeismicState();
	}

	@Override
	protected QSeismicMessage createMessage() {
		return new QSeismicMessage();
	}

	@Override
	public void init() {
		for(int i=0; i<D; i++)
			for(int j=0; j<D; j++) {
				int index = makeIndex(i, j);
				s.changed[index] = false;
				s.dist[index] = -1;
			}
		s.pending = 0;
		s.parentx = -1;
		s.parenty = -1;
		if(s.isSource) {
			s.dist[MID] = 0;
			s.changed[MID] = true;
			s.pending = 1;
			readyToSend = pin(0);
		}
		else {
			readyToSend = NO;
		}
	}

	@Override
	public void send(QSeismicMessage msg) {
		for(int i=0; i<D; i++)
			for(int j=0; j<D; j++) {
				int index = makeIndex(i, j);
				if(s.changed[index]) {
					msg.x = s.x + (i-R);
					msg.y = s.y + (j-R);
					msg.dist = s.dist[index];
					s.changed[index] = false;
					s.pending--;
					readyToSend = s.pending>0 ? pin(0) : NO;
					return;
				}
			}
		readyToSend = NO;
	}

	private boolean updateDist(int index, int newDist) {
		if(s.dist[index]<0 || s.dist[index]>newDist) {
			s.dist[index]	= newDist;
			if(!s.changed[index]) {
				s.changed[index] = true;
				s.pending++;
			}
			return true;
		}
		else
			return false;
	}
	
	@Override
	public void recv(QSeismicMessage msg, Void weight) {
		int i = (msg.x - s.x) + R;
		int j = (msg.y - s.y) + R;
		if(i>=0 && i<D && j>=0 && j<D) {
			int index = makeIndex(i, j);
			if(updateDist(index, msg.dist)) {
				if(s.w[index]>0) {
					if(updateDist(MID, msg.dist + s.w[index])) {
						s.parentx = msg.x;
						s.parenty = msg.y;
					}
				}
			}
		}
		readyToSend = NO;
	}

	@Override
	public boolean step() {
		if(s.pending>0) {
			readyToSend = pin(0);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean finish(QSeismicMessage msg) {
		msg.x = s.parentx;
		msg.y = s.parenty;
		msg.dist = s.dist[MID];
		return true;
	}

}
