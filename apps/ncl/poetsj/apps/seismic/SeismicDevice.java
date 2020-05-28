package ncl.poetsj.apps.seismic;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicMessage;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicState;

public class SeismicDevice extends PDevice<SeismicState, Integer, SeismicMessage> {

	public static class SeismicGraph extends PGraph<SeismicDevice, SeismicState, Integer, SeismicMessage> {
		@Override
		protected SeismicDevice createDevice() {
			return new SeismicDevice();
		}
	}
	
	public static class SeismicState {
		public int node;
		public boolean isSource;
		public boolean changed;
		public int parent;
		public int dist;
	}

	public static class SeismicMessage {
		public int node;
		public int from;
		public int dist;
	}

	@Override
	protected SeismicState createState() {
		return new SeismicState();
	}

	@Override
	protected SeismicMessage createMessage() {
		return new SeismicMessage();
	}

	@Override
	public void init() {
		readyToSend = s.isSource ? pin(0) : NO;
	}

	@Override
	public void send(SeismicMessage msg) {
		msg.node = s.node;
		msg.from = s.node;
		msg.dist = s.dist;
		readyToSend = NO;
	}

	@Override
	public void recv(SeismicMessage msg, Integer weight) {
		int newDist = msg.dist + weight;
		if(s.dist<0 || newDist<s.dist) {
			s.parent = msg.from;
			s.dist = newDist;
			s.changed = true;
		}
	}

	@Override
	public boolean step() {
		if(s.changed) {
			s.changed = false;
			readyToSend = pin(0);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean finish(SeismicMessage msg) {
		msg.node = s.node;
		msg.from = s.parent;
		msg.dist = s.dist;
		return true;
	}
	
}
