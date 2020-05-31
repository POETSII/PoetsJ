package ncl.poetsj.apps.denqseismic;

import static java.lang.System.*;
import static ncl.poetsj.apps.qseismic.QSeismicDevice.*;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.FileInputStream;

import com.xrbpowered.zoomui.GraphAssist;
import com.xrbpowered.zoomui.KeyInputHandler;
import com.xrbpowered.zoomui.UIContainer;
import com.xrbpowered.zoomui.UIWindow;
import com.xrbpowered.zoomui.std.UIButton;
import com.xrbpowered.zoomui.swing.SwingWindowFactory;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.denqseismic.DenQSeismicDevice.DenQSeismicGraph;
import ncl.poetsj.apps.denqseismic.DenQSeismicDevice.DenQSeismicMessage;
import ncl.poetsj.apps.denqseismic.DenQSeismicDevice.DenQSeismicState;

public class DenQSeismicRun {

public static boolean debug = false;
	
	public static final int cellSize = 40;
	
	public static class DenQSeismicDebugWindow extends UIContainer implements KeyInputHandler {
		
		public final int w, h;
		public final DenQSeismicGraph graph;
		public boolean done = false;
		
		private int hoveri, hoverj;
		
		public DenQSeismicDebugWindow(UIContainer parent, DenQSeismicGraph graph, int w, int h) {
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
			
			if(hoveri>=0 && hoveri<w && hoverj>=0 && hoverj<h) {
				DenQSeismicState dev = graph.devices.get(hoverj*w+hoveri).s;
				
				for(int rj=-R; rj<=R; rj++)
					for(int ri=-R; ri<=R; ri++) {
						int i = ri+R;
						int j = rj+R;
						int index = DenQSeismicDevice.makeIndex(i, j);
						
						int x = hoveri+ri;
						int y = hoverj+rj;
						
						g.setColor(ri==0 && rj==0 ? new Color(0x77ff77) : new Color(0xeeffee));
						g.fillRect(x*cellSize, y*cellSize, cellSize, cellSize);
						g.setColor(Color.BLACK);
						String s = String.format("%d", dev.w[index]);
						g.drawString(s, x*cellSize+cellSize/2, y*cellSize+cellSize/2, GraphAssist.CENTER, GraphAssist.CENTER);
					}
			}
		}
		
		@Override
		public void onMouseMoved(float x, float y, int mods) {
			hoveri = (int)(x / cellSize);
			hoverj = (int)(y / cellSize);
			repaint();
		}
		
		@Override
		public boolean onMouseDown(float x, float y, Button button, int mods) {
			/*if(!done) {
				done = !graph.step();
				repaint();
			}*/
			return true;
		}

		@Override
		public boolean onKeyPressed(char c, int code, int mods) {
			/*if(code==KeyEvent.VK_SPACE) {
				if(!done) {
					done = !graph.step();
					repaint();
				}
			}*/
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
			out.printf("Loading density map...\n");
			DataInputStream in = new DataInputStream(new FileInputStream(args[0]));
			int w = in.readInt();
			int h = in.readInt();

			int[][] den = new int[w][h];
			for(int y=0; y<h; y++) {
				for(int x=0; x<w; x++) {
					den[x][y] = in.readByte() & 0xff;
				}
			}
			in.close();
			
			// create graph
			out.printf("Creating devices...\n");
			DenQSeismicGraph graph = new DenQSeismicGraph();
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
			
			out.printf("Nodes: %d\n", w*h);
			
			// read edge weights and create edges
			int rootNode = 2*w+w/2; // TODO: source id from arg
			for(int sj=0; sj<h; sj++)
				for(int si=0; si<w; si++) {
					int s = sj*w+si;
					DenQSeismicState dev = graph.devices.get(s).s;
					dev.x = si;
					dev.y = sj;
					dev.isSource = (s==rootNode);
					
					for(int rj=-R; rj<=R; rj++)
						for(int ri=-R; ri<=R; ri++) {
							int i = ri+R;
							int j = rj+R;
							int index = DenQSeismicDevice.makeIndex(i, j);
							
							int di = si+ri;
							if(di<0) di = 0;
							if(di>=w) di = w-1;
							int dj = sj+rj;
							if(dj<0) dj = 0;
							if(dj>=h) dj = h-1;
							
							dev.changed[index] = den[di][dj];
						}
				}

			if(debug) {
				UIWindow frame = SwingWindowFactory.use(1f).createFrame("DenQSeismicDebugWindow", w*cellSize, h*cellSize);
				new DenQSeismicDebugWindow(frame.getContainer(), graph, w, h);
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
					DenQSeismicMessage msg = graph.hostMessages.get(i).msg;
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
