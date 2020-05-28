package ncl.poetsj;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class PGraph<D extends PDevice<S, E, M>, S, E, M> {

	public static boolean logMessageCount = false;
	
	public static int totalSteps = 0;
	
	public final ArrayList<D> devices = new ArrayList<>();
	public final LinkedList<PDevice<S, E, M>.PMessage> hostMessages = new LinkedList<>();
	
	protected abstract D createDevice();
	
	public int newDevice() {
		devices.add(createDevice());
		return devices.size()-1;
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

	public void go() {
		for(PDevice<S, E, M> dev : devices) {
			dev.init();
		}
		boolean active = true;
		do {
			active = false;
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
			if(logMessageCount)
				System.out.printf("Messages sent: %.1fM\n", PDevice.totalMessageCount/1000000.0);
		} while(active);
	}
	
}
