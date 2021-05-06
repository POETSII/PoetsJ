package ncl.poetsj;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public abstract class PDevice<S, E, M> {

	public static boolean randomMessageOrder = true;
	public static boolean logMessages = false;
	public static int totalMessageCount = 0;
	
	public static final int NO = 0; 
	
	public static int pin(int p) {
		return (1<<p);
	}
	
	protected class PEdge {
		public int pin;
		public PDevice<S, E, M> dest;
		public E label;
	}
	
	public class PMessage {
		public M msg;
		public E edge;
	}
	
	public int deviceId;
	public int readyToSend = NO;
	
	public final S s = createState();
	
	protected abstract S createState();
	protected abstract M createMessage();
	
	public abstract void init();
	public abstract void send(M msg);
	public abstract void recv(M msg, E edge);
	public abstract boolean step();
	public abstract boolean finish(M msg);

	protected HashMap<PDevice<S, E, M>, PEdge> edges = new HashMap<>();
	protected LinkedList<PMessage> mailbox = new LinkedList<>();
	
	protected void addLabelledEdge(E label, int pin, PDevice<S, E, M> dest) {
		if(!edges.containsKey(dest)) {
			PEdge edge = new PEdge();
			edge.pin = pin;
			edge.label = label;
			edge.dest = dest;
			edges.put(dest, edge);
		}
	}
	
	private static final Random random = new Random();
	
	protected boolean update() {
		while(!mailbox.isEmpty()) {
			PMessage m;
			if(randomMessageOrder) {
				int index = random.nextInt(mailbox.size());
				m = mailbox.remove(index);
			}
			else
				m = mailbox.pop();
			if(logMessages)
				System.out.printf("%d:recv(%s)\n", deviceId, m.msg.toString());
			recv(m.msg, m.edge);
		}
		if(readyToSend!=NO) {
			M msg = createMessage();
			send(msg);
			if(logMessages)
				System.out.printf("%d:send(%s)\n", deviceId, msg.toString());
			for(PEdge edge : edges.values()) {
				PMessage m = new PMessage();
				m.msg = msg;
				m.edge = edge.label;
				edge.dest.mailbox.add(m);
				totalMessageCount++;
			}
			return true;
		}
		else
			return false;
	}
	
	protected PMessage createPMessage() {
		PMessage m = new PMessage();
		m.msg = createMessage();
		return m;
	}
	
}
