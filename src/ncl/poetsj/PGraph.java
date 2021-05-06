package ncl.poetsj;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class PGraph<D extends PDevice<S, E, M>, S, E, M> {

	public static int totalSteps = 0;
	
	public final ArrayList<D> devices = new ArrayList<>();
	public final LinkedList<PDevice<S, E, M>.PMessage> hostMessages = new LinkedList<>();
	
	protected abstract D createDevice();
	
	public int newDevice() {
		D dev = createDevice();
		dev.deviceId = devices.size();
		devices.add(dev);
		return dev.deviceId;
	}
	
	public int numDevices() {
		return devices.size();
	}
	
	public void addEdge(int from, int pin, int to) {
		addLabelledEdge(null, from, pin, to);
	}

	public void addLabelledEdge(E edge, int from, int pin, int to) {
		devices.get(from).addLabelledEdge(edge, pin, devices.get(to));
	}

	public void init() {
		for(PDevice<S, E, M> dev : devices) {
			dev.init();
		}
	}
	
	public boolean step() {
		boolean active = false;
		for(PDevice<S, E, M> dev : devices) {
			active |= dev.update();
		}
		if(!active) {
			totalSteps++;
			for(PDevice<S, E, M> dev : devices) {
				active |= dev.step();
			}
		}
		if(!active) {
			hostMessages.clear();
			for(PDevice<S, E, M> dev : devices) {
				PDevice<S, E, M>.PMessage m = dev.createPMessage();
				active |= !dev.finish(m.msg);
				hostMessages.add(m);
			}
		}
		return active;
	}
	
	public void go() {
		init();
		while(step()) {}
	}
	
}
