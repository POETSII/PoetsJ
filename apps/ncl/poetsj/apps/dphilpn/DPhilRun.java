package ncl.poetsj.apps.dphilpn;

import static java.lang.System.out;
import static ncl.poetsj.apps.dphilpn.DPhilDevice.localPlaces;
import static ncl.poetsj.apps.dphilpn.DPhilDevice.outPlaces;
import static ncl.poetsj.apps.dphilpn.DPhilDevice.transitions;

import java.awt.Color;

import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIElement;
import com.xrbpowered.zoomui.UIWindow;
import com.xrbpowered.zoomui.std.UIButton;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

import ncl.poetsj.PGraph;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilGraph;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilMessage;
import ncl.poetsj.apps.dphilpn.DPhilDevice.DPhilState;
import ncl.poetsj.apps.dphilpn.DPhilDevice.OutMap;

public class DPhilRun {

	public static boolean display = true;
	
	public static int[] initPlaces;
	public static int tileInitPlaces;
	public static OutMap[] tOutMap;

	public static final int DEFAULT_NUM_DEVS = 5;
	public static final int DEFAULT_TIME_LIMIT = 100;

	public static boolean TIME_LIMIT_TOTAL = false;
	public static final long SEED = 12345L;

	public static int[] highlightPlaces = {7, 13};
	
	public static class DPhilDisplay extends UIContainer {
		public final DPhilGraph graph;
		public boolean done = false;
		public boolean running = false;
		public final Thread thread;
		
		public DPhilDisplay(UIContainer parent, DPhilGraph graph) {
			super(parent);
			this.graph = graph;
			graph.init();
			thread = new Thread() {
				public void run() {
					try {
						while(!done) {
							if(running) {
								done = !graph.step();
								repaint();
							}
							Thread.sleep(50);
						}
					}
					catch(InterruptedException e) {
					}
				}
			};
			thread.start();
		}
		
		@Override
		protected void paintSelf(GraphAssist g) {
			g.fill(this, new Color(0x555555));
			g.setColor(Color.WHITE);
			g.setFont(UIButton.font);
			g.drawString(Integer.toString(PGraph.totalSteps)+(done? " (done)" : ""), 16, 16);
			for(int i=0; i<highlightPlaces.length; i++) {
				g.fillRect(highlightPlaces[i]*16+4, 20, 5, 5);
			}
			for(int d=0; d<graph.devices.size(); d++) {
				DPhilState dev = graph.devices.get(d).s;
				for(int p=0; p<localPlaces+outPlaces; p++) {
					g.setColor(dev.places[p]>0 ? new Color(0x77dd33) : Color.BLACK);
					g.fillRect(p*16, d*16+28, 14, 7);
					g.setColor(dev.changed[p] ? new Color(0xffdd55) : Color.BLACK);
					g.fillRect(p*16, d*16+35, 14, 7);
				}
			}
		}
		
		@Override
		public boolean onMouseDown(float x, float y, Button button, int mods) {
			if(button==Button.right) {
				running = true;
			}
			else {
				if(!done) {
					done = !graph.step();
					repaint();
				}
			}
			return true;
		}
		
		@Override
		public boolean onMouseUp(float x, float y, Button button, int mods, UIElement initiator) {
			running = false;
			return true;
		}
	}
	
	public static void main(String[] args) {
		DPhilTransitionMap.create(1);
		
		int numDevs = DEFAULT_NUM_DEVS;
		if(args.length>0) {
			numDevs = Integer.parseInt(args[0]);
		}
		out.printf("numDevs: %d (total places: %.2fM)\n", numDevs, (double)(numDevs*(localPlaces+outPlaces/2))/1.0e6);
		
		long timeLimit, timeLimitPerDev, timeLimitRemainder;
		if(TIME_LIMIT_TOTAL) {
			timeLimit = DEFAULT_TIME_LIMIT;
			if(args.length>1) {
				timeLimit = Long.parseLong(args[1]);
			}
			timeLimitPerDev = timeLimit / numDevs;
			timeLimitRemainder = timeLimit - timeLimitPerDev*numDevs;
		}
		else {
			timeLimitPerDev = DEFAULT_TIME_LIMIT;
			if(args.length>1) {
				timeLimitPerDev = Long.parseLong(args[1]);
			}
			timeLimitPerDev *= (long)transitions;
			timeLimit = timeLimitPerDev*numDevs;
			timeLimitRemainder = 0;
		}
		out.printf("timeLimit: %d (%d/dev, %d/tran)\n", timeLimit, timeLimitPerDev, timeLimitPerDev/(long)transitions);

		long startAll, finishAll;
		startAll = System.currentTimeMillis();

		out.printf("Creating graph...\n");
		DPhilGraph graph = new DPhilGraph();
		for(int i=0; i<numDevs; i++) {
			graph.newDevice();
		}
		
		for(int i=0; i<numDevs; i++) {
			graph.addEdge(i, 0, i>0 ? i-1 : numDevs-1);
			graph.addEdge(i, 0, i<numDevs-1 ? i+1 : 0);
		}

		long seed = SEED;
		for(int i=0; i<numDevs; i++) {
			DPhilState dev = graph.devices.get(i).s;
			dev.dev = i;
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			dev.seed = seed + (i*23);
			dev.timeLimit = (int)(timeLimitPerDev + (i<timeLimitRemainder ? 1 : 0));
			for(int p=0; p<localPlaces+outPlaces; p++) {
				if(p<localPlaces)
					dev.places[p] = initPlaces[p%tileInitPlaces];
				else
					dev.places[p] = 0;
			}
			
			int prev = i>0 ? i-1 : numDevs-1;
			int next = i<numDevs-1 ? i+1 : 0;
			for(int p=0; p<outPlaces; p++) {
				dev.outMap[p].dev = tOutMap[p].dev==1 ? next : prev;
				dev.outMap[p].place = tOutMap[p].place;
			}
		}

		if(display) {
			UIWindow frame = SwingWindowFactory.use(1f).createFrame("QSeismicDebugWindow", 800, 800);
			new DPhilDisplay(frame.getContainer(), graph);
			frame.show();
		}
		else {
			out.printf("\nStarted\n");
			long startCompute, finishCompute = 0L;
			startCompute = System.currentTimeMillis();
			graph.go();
	
			for(int i = 0; i < graph.hostMessages.size(); i++) {
				DPhilMessage msg = graph.hostMessages.get(i).msg;
				if(i == 0) {
					finishCompute = System.currentTimeMillis();
					out.printf("%d steps\n", msg.dev);
				}
			}
	
			long diff;
			double duration;
			diff = finishCompute - startCompute;
			duration = diff / 1000.0;
			out.printf("Time (compute) = %.3f\n", duration);
			finishAll = System.currentTimeMillis();
			diff = finishAll - startAll;
			duration = diff / 1000.0;
			out.printf("Time (all) = %.3f\n", duration);
		}
	}

}
