package ncl.poetsj.apps.denqseismic;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.denqseismic.DenQSeismicDevice.DenQSeismicMessage;
import ncl.poetsj.apps.denqseismic.DenQSeismicDevice.DenQSeismicState;

public class DenQSeismicDevice extends PDevice<DenQSeismicState, Void, DenQSeismicMessage> {

	public static final int R = 5;
	public static final int D = R*2+1;
	public static final int MID = R*D+R;

	public static final float LINE_STEP = 1f;
	public static final float BIAS = 0.25f;
	public static final float SCALE = 1f;
	public static final float PREC = 1000f;
	
	public static int makeIndex(int i, int j) {
		return i*D+j;
	}
	
	public static class DenQSeismicGraph extends PGraph<DenQSeismicDevice, DenQSeismicState, Void, DenQSeismicMessage> {
		@Override
		protected DenQSeismicDevice createDevice() {
			return new DenQSeismicDevice();
		}
	}
	
	public static class DenQSeismicState {
		public int x, y;
		public boolean isSource;
		public int[] w = new int[D*D];
		public int pending;
		public int[] changed = new int[D*D]; // also used as density before init()
		public int[] dist = new int[D*D];
		public int parentx, parenty;
	}

	public static class DenQSeismicMessage {
		public int x, y;
		public int dist;
	}

	@Override
	protected DenQSeismicState createState() {
		return new DenQSeismicState();
	}

	@Override
	protected DenQSeismicMessage createMessage() {
		return new DenQSeismicMessage();
	}

	public float d(int i, int j) {
		return (1f-s.changed[makeIndex(i, j)]/255f)*SCALE+BIAS;
	}
	
	public float get(float x, float y) {
		int x0 = (int)Math.floor(x);
		int x1 = x0+1;
		if(x1>=D) x1 = x0;
		float sx = x-x0;
		int y0 = (int)Math.floor(y);
		int y1 = y0+1;
		if(y1>=D) y1 = y0;
		float sy = y-y0;
		float d00 = d(x0, y0);
		float d01 = d(x0, y1);
		float d10 = d(x1, y0);
		float d11 = d(x1, y1);
		return lerp(lerp(d00, d01, sy), lerp(d10, d11, sy), sx);
	}
	
	public float line(float x0, float y0, float x1, float y1) {
		float dx = x1-x0;
		float dy = y1-y0;
		float dist = (float)Math.sqrt(dx*dx+dy*dy);
		int steps = (int)(dist*LINE_STEP);
		float sum = 0;
		float prev = get(x0, y0);
		for(int i=1; i<=steps; i++) {
			float s = i / (float)steps;
			float x = lerp(x0, x1, s);
			float y = lerp(y0, y1, s);
			float v = get(x, y);
			sum += (prev+v)/2;
			prev = v;
		}
		return sum * dist / (float)steps;
	}
	
	public static float lerp(float x0, float x1, float s) {
		return x0 * (1-s) + x1 * s;
	}
	
	@Override
	public void init() {
		for(int i=0; i<D; i++)
			for(int j=0; j<D; j++) {
				int index = makeIndex(i, j);
				if(i!=R || j!=R) {
					float x = line(R, R, i, j);
					s.w[index] = Math.round(x*PREC);
				}
				else {
					s.w[index] = 0;
				}
			}
		
		for(int i=0; i<D; i++)
			for(int j=0; j<D; j++) {
				int index = makeIndex(i, j);
				s.changed[index] = 0;
				s.dist[index] = -1;
			}
		s.pending = 0;
		s.parentx = -1;
		s.parenty = -1;
		if(s.isSource) {
			s.dist[MID] = 0;
			s.changed[MID] = 1;
			s.pending = 1;
			readyToSend = pin(0);
		}
		else {
			readyToSend = NO;
		}
	}

	@Override
	public void send(DenQSeismicMessage msg) {
		for(int i=0; i<D; i++)
			for(int j=0; j<D; j++) {
				int index = makeIndex(i, j);
				if(s.changed[index]!=0) {
					msg.x = s.x + (i-R);
					msg.y = s.y + (j-R);
					msg.dist = s.dist[index];
					s.changed[index] = 0;
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
			if(s.changed[index]==0) {
				s.changed[index] = 1;
				s.pending++;
			}
			return true;
		}
		else
			return false;
	}
	
	@Override
	public void recv(DenQSeismicMessage msg, Void weight) {
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
	public boolean finish(DenQSeismicMessage msg) {
		msg.x = s.parentx;
		msg.y = s.parenty;
		msg.dist = s.dist[MID];
		return true;
	}

}
