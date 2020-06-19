package ncl.poetsj.apps.denqseismic3d;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.denqseismic3d.DenQSeismic3DDevice.DenQSeismic3DMessage;
import ncl.poetsj.apps.denqseismic3d.DenQSeismic3DDevice.DenQSeismic3DState;

public class DenQSeismic3DDevice extends PDevice<DenQSeismic3DState, Void, DenQSeismic3DMessage> {

	public static final int R = 1;
	public static final int D = R*2+1;
	public static final int MID = (R*D+R)*D+R;

	public static final float LINE_STEP = 1f;
	public static final float BIAS = 0.25f;
	public static final float SCALE = 1f;
	public static final float PREC = 1000f;
	
	public static int makeIndex(int i, int j, int k) {
		return (k*D+i)*D+j;
	}
	
	public static class DenQSeismic3DGraph extends PGraph<DenQSeismic3DDevice, DenQSeismic3DState, Void, DenQSeismic3DMessage> {
		@Override
		protected DenQSeismic3DDevice createDevice() {
			return new DenQSeismic3DDevice();
		}
	}
	
	public static class DenQSeismic3DState {
		public int x, y, z;
		public boolean isSource;
		public int[] w = new int[D*D*D];
		public int pending;
		public int[] changed = new int[D*D*D]; // also used as density before init()
		public int[] dist = new int[D*D*D];
		public int parentx, parenty, parentz;
	}

	public static class DenQSeismic3DMessage {
		public int x, y, z;
		public int dist;
	}

	@Override
	protected DenQSeismic3DState createState() {
		return new DenQSeismic3DState();
	}

	@Override
	protected DenQSeismic3DMessage createMessage() {
		return new DenQSeismic3DMessage();
	}

	public float d(int i, int j, int k) {
		return (1f-s.changed[makeIndex(i, j, k)]/255f)*SCALE+BIAS;
	}
	
	public float get(float x, float y, float z) {
		int x0 = (int)Math.floor(x);
		int x1 = x0+1;
		if(x1>=D) x1 = x0;
		float sx = x-x0;
		int y0 = (int)Math.floor(y);
		int y1 = y0+1;
		if(y1>=D) y1 = y0;
		float sy = y-y0;
		int z0 = (int)Math.floor(z);
		int z1 = z0+1;
		if(z1>=D) z1 = z0;
		float sz = z-z0;
		float d000 = d(x0, y0, z0);
		float d010 = d(x0, y1, z0);
		float d100 = d(x1, y0, z0);
		float d110 = d(x1, y1, z0);
		float d001 = d(x0, y0, z1);
		float d011 = d(x0, y1, z1);
		float d101 = d(x1, y0, z1);
		float d111 = d(x1, y1, z1);
		return lerp(
				lerp(lerp(d000, d010, sy), lerp(d100, d110, sy), sx),
				lerp(lerp(d001, d011, sy), lerp(d101, d111, sy), sx),
				sz
			);
	}
	
	public float line(float x0, float y0, float z0, float x1, float y1, float z1) {
		float dx = x1-x0;
		float dy = y1-y0;
		float dz = z1-z0;
		float dist = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
		int steps = (int)(dist*LINE_STEP);
		float sum = 0;
		float prev = get(x0, y0, z0);
		for(int i=1; i<=steps; i++) {
			float s = i / (float)steps;
			float x = lerp(x0, x1, s);
			float y = lerp(y0, y1, s);
			float z = lerp(z0, z1, s);
			float v = get(x, y, z);
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
		for(int k=0; k<D; k++)
			for(int j=0; j<D; j++)
				for(int i=0; i<D; i++) {
					int index = makeIndex(i, j, k);
					if(i!=R || j!=R) {
						float x = line(R, R, R, i, j, k);
						s.w[index] = Math.round(x*PREC);
					}
					else {
						s.w[index] = 0;
					}
				}
		
		for(int k=0; k<D; k++)
			for(int j=0; j<D; j++)
				for(int i=0; i<D; i++) {
					int index = makeIndex(i, j, k);
					s.changed[index] = 0;
					s.dist[index] = -1;
				}
		s.pending = 0;
		s.parentx = -1;
		s.parenty = -1;
		s.parentz = -1;
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
	public void send(DenQSeismic3DMessage msg) {
		for(int k=0; k<D; k++)
			for(int j=0; j<D; j++)
				for(int i=0; i<D; i++) {
					int index = makeIndex(i, j, k);
					if(s.changed[index]!=0) {
						msg.x = s.x + (i-R);
						msg.y = s.y + (j-R);
						msg.z = s.z + (k-R);
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
	public void recv(DenQSeismic3DMessage msg, Void weight) {
		int i = (msg.x - s.x) + R;
		int j = (msg.y - s.y) + R;
		int k = (msg.z - s.z) + R;
		if(i>=0 && i<D && j>=0 && j<D && k>=0 && k<D) {
			int index = makeIndex(i, j, k);
			if(updateDist(index, msg.dist)) {
				if(s.w[index]>0) {
					if(updateDist(MID, msg.dist + s.w[index])) {
						s.parentx = msg.x;
						s.parenty = msg.y;
						s.parentz = msg.z;
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
	public boolean finish(DenQSeismic3DMessage msg) {
		msg.x = s.parentx;
		msg.y = s.parenty;
		msg.z = s.parentz;
		msg.dist = s.dist[MID];
		return true;
	}

}
