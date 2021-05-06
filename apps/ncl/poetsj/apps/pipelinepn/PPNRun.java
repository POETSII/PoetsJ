package ncl.poetsj.apps.pipelinepn;

import static java.lang.System.out;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.MAX_PLACES;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.localPlaces;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.outPlaces;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.transitions;

import java.awt.Color;

import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIElement;
import com.xrbpowered.zoomui.UIWindow;
import com.xrbpowered.zoomui.std.UIButton;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

import ncl.poetsj.PGraph;
import ncl.poetsj.apps.pipelinepn.PPNDevice.PPNGraph;
import ncl.poetsj.apps.pipelinepn.PPNDevice.PPNMessage;
import ncl.poetsj.apps.pipelinepn.PPNDevice.PPNState;

public class PPNRun {

	public static boolean display = true;
	
	public static final int[] initPlaces = {1, 1, 0, 0, 0, 1, 1, 0, 0};
	public static final int tileInitPlaces = 9;
	
	public static final int DEFAULT_NUM_DEVS = 20;
	public static final int DEFAULT_TIME_LIMIT = 1000;
	public static final int WATCH_PLACE = 0;

	public static boolean TIME_LIMIT_TOTAL = false;

	public static class PPNDisplay extends UIContainer {
		public final PPNGraph graph;
		public boolean done = false;
		public boolean running = false;
		public final Thread thread;
		
		public PPNDisplay(UIContainer parent, PPNGraph graph) {
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
			for(int d=0; d<graph.devices.size(); d++) {
				PPNState dev = graph.devices.get(d).s;
				for(int p=0; p<localPlaces+outPlaces; p++) {
					g.setColor(dev.places[p]>0 ? new Color(0x77dd33) : Color.BLACK);
					g.fillRect(p*16, d*16+20, 14, 7);
					g.setColor(dev.placesUpd[p]>0 ? new Color(0x99ff55) : Color.BLACK);
					g.fillRect(p*16, d*16+27, 14, 7);
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
		PPNTransitionMap.create(4);
		
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
		PPNGraph graph = new PPNGraph();
		for(int i=0; i<numDevs; i++) {
			graph.newDevice();
		}
		
		for(int i=0; i<numDevs; i++) {
			graph.addEdge(i, 0, i>0 ? i-1 : numDevs-1);
			graph.addEdge(i, 0, i<numDevs-1 ? i+1 : 0);
		}

		for(int i=0; i<numDevs; i++) {
			PPNState dev = graph.devices.get(i).s;
			dev.dev = i;
			dev.timeLimit = (int)(timeLimitPerDev + (i<timeLimitRemainder ? 1 : 0));
			dev.watchPlace = WATCH_PLACE;
			for(int p=0; p<MAX_PLACES; p++) {
				if(p<localPlaces)
					dev.places[p] = initPlaces[p%tileInitPlaces];
				else
					dev.places[p] = 0;
			}
			
			dev.outMap[0].dev = i<numDevs-1 ? i+1 : 0;
			dev.outMap[0].place = localPlaces+2;
			dev.outMap[1].dev = i<numDevs-1 ? i+1 : 0;
			dev.outMap[1].place = localPlaces+3;
			dev.outMap[2].dev = i>0 ? i-1 : numDevs-1;
			dev.outMap[2].place = localPlaces+0;
			dev.outMap[3].dev = i>0 ? i-1 : numDevs-1;
			dev.outMap[3].place = localPlaces+1;
		}

		if(display) {
			UIWindow frame = SwingWindowFactory.use(1f).createFrame("QSeismicDebugWindow", 800, 800);
			new PPNDisplay(frame.getContainer(), graph);
			frame.show();
		}
		else {
			out.printf("\nStarted\n");
			long startCompute, finishCompute = 0L;
			startCompute = System.currentTimeMillis();
			graph.go();
	
			for(int i = 0; i < graph.hostMessages.size(); i++) {
				PPNMessage msg = graph.hostMessages.get(i).msg;
				if(i == 0) {
					finishCompute = System.currentTimeMillis();
					out.printf("%d steps, place[%d]=%d\n", msg.dev, WATCH_PLACE, msg.st);
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
