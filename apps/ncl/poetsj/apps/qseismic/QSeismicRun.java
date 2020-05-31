package ncl.poetsj.apps.qseismic;

import static java.lang.System.*;
import static ncl.poetsj.apps.qseismic.QSeismicDevice.*;

import java.awt.Color;
import java.io.File;
import java.util.Scanner;

import com.sun.glass.events.KeyEvent;
import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.KeyInputHandler;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIWindow;
import com.xrbpowered.zoomui.std.UIButton;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.qseismic.QSeismicDevice.QSeismicGraph;
import ncl.poetsj.apps.qseismic.QSeismicDevice.QSeismicMessage;
import ncl.poetsj.apps.qseismic.QSeismicDevice.QSeismicState;

public class QSeismicRun {

	public static boolean debug = false;
	
	public static final int cellSize = 40;
	
	public static class QSeismicDebugWindow extends UIContainer implements KeyInputHandler {
		
		public final int w, h;
		public final QSeismicGraph graph;
		public boolean done = false;
		
		public QSeismicDebugWindow(UIContainer parent, QSeismicGraph graph, int w, int h) {
			super(parent);
			this.graph = graph;
			this.w = w;
			this.h = h;
			graph.init();
			getBase().setFocus(this);
		}
		
		@Override
		protected void paintSelf(GraphAssist g) {
			g.fill(this, Color.WHITE);
			g.setFont(UIButton.font);
			for(int j=0; j<h; j++) {
				for(int i=0; i<w; i++) {
					QSeismicState dev = graph.devices.get(j*w+i).s;
					g.setColor(Color.BLACK);
					String s = String.format("%d", dev.dist[MID]);
					g.drawString(s, i*cellSize+cellSize/2, j*cellSize+cellSize/2, GraphAssist.CENTER, GraphAssist.CENTER);
					if(dev.parentx>=0 && dev.parenty>=0) {
						g.setColor(Color.RED);
						g.line(i*cellSize+cellSize/2, j*cellSize+cellSize/2, dev.parentx*cellSize+cellSize/2, dev.parenty*cellSize+cellSize/2);
					}
				}
			}
		}
		
		@Override
		public boolean onMouseDown(float x, float y, Button button, int mods) {
			if(!done) {
				done = !graph.step();
				repaint();
			}
			return true;
		}

		@Override
		public boolean onKeyPressed(char c, int code, int mods) {
			if(code==KeyEvent.VK_SPACE) {
				if(!done) {
					done = !graph.step();
					repaint();
				}
			}
			return true;
		}

		@Override
		public void onFocusGained() {
		}

		@Override
		public void onFocusLost() {
		}
	}
	
	public static void main(String[] args) {
		try {
			if(args.length!=1) {
				out.printf("Specify input file\n");
				exit(1);
			}
			
			long startAll = currentTimeMillis();
		
			// Read input
			Scanner in = new Scanner(new File(args[0]));
			
			int w = in.nextInt();
			int h = in.nextInt();
			in.nextInt(); // step
			
			// create graph
			out.printf("Loading...\n");
			QSeismicGraph graph = new QSeismicGraph();
			for(int j=0; j<h; j++) {
				for(int i=0; i<w; i++) {
					int id = graph.newDevice();
					assert(j*w+i == id);
				}
			}
			
			for(int j = 0; j<h; j++) {
				for(int i = 0; i<w; i++) {
					int src = j*w+i;
					if(i>0) graph.addEdge(src, 0, src-1);
					if(i<w-1) graph.addEdge(src, 0, src+1);
					if(j>0) graph.addEdge(src, 0, src-w);
					if(j<h-1) graph.addEdge(src, 0, src+w);
				}
			}
			
			// read fanout template
			int r = in.nextInt();
			if(r!=R) {
				out.printf("Expected radius %d, got %d\n", R, r);
				exit(1);
			}
			in.nextInt(); // maxFanout
			boolean[][] e = new boolean[r+1][r+1]; 
			for(int ri=0; ri<=r; ri++)
				for(int rj=0; rj<=r; rj++) {
					e[ri][rj] = in.nextInt()!=0;
				}
			
			int totalEdges = in.nextInt();
			out.printf("Nodes: %d\nEdges: %d\n", w*h, totalEdges);

			int maxw = 0;
			// read edge weights and create edges
			int rootNode = 2*w+w/2; // TODO: source id from arg
			for(int sj=0; sj<h; sj++)
				for(int si=0; si<w; si++) {
					int s = sj*w+si;
					QSeismicState dev = graph.devices.get(s).s;
					dev.x = si;
					dev.y = sj;
					dev.isSource = (s==rootNode);
					
					for(int rj=-r; rj<=r; rj++)
						for(int ri=-r; ri<=r; ri++) {
							int i = ri+R;
							int j = rj+R;
							int index = QSeismicDevice.makeIndex(i, j);
							
							if(e[Math.abs(ri)][Math.abs(rj)]) {
								int di = si+ri;
								int dj = sj+rj;
								if(di<0 || dj<0 || di>=w || dj>=h) {
									dev.w[index] = -1;
								}
								else {
									int x = in.nextInt();
									if(x>maxw)
										maxw = x;
									dev.w[index] = x;
									totalEdges--;
	
									//printf("%d %d %.3f\n", s, d, x);
								}
							}
							else
								dev.w[index] = -1;
						}
				}
			out.printf("max w = %d\n", maxw);

			in.close();
			assert(totalEdges==0);

			if(debug) {
				UIWindow frame = SwingWindowFactory.use(1f).createFrame("QSeismicDebugWindow", w*cellSize, h*cellSize);
				new QSeismicDebugWindow(frame.getContainer(), graph, w, h);
				frame.show();
			}
			else {
				out.printf("\nStarted\n");
				long startCompute, finishCompute;
				startCompute = currentTimeMillis();
				graph.go();
				finishCompute = currentTimeMillis();
				
				long sum = 0;
				// Accumulate sum at each device
				for(int i = 0; i < graph.hostMessages.size(); i++) {
					QSeismicMessage msg = graph.hostMessages.get(i).msg;
					//out.printf("%d %d %d\n", msg.node, msg.from, msg.dist);
					sum += msg.dist;
				}
				out.printf("sum = %d\n", sum);
				
				double duration;
				duration = (finishCompute - startCompute) / 1000.0;
				out.printf("Time (compute) = %.3f\n", duration);
				long finishAll = currentTimeMillis();
				duration = (finishAll - startAll) / 1000.0;
				out.printf("Time (all) = %.3f\n", duration);
				
				out.printf("Total messages: %d\nTotal steps: %d\n", PDevice.totalMessageCount, PGraph.totalSteps);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			exit(1);
		}
	}

}
