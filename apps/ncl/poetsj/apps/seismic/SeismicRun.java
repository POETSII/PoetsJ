package ncl.poetsj.apps.seismic;

import static java.lang.System.*;

import java.io.File;
import java.util.Scanner;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicGraph;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicMessage;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicState;

public class SeismicRun {

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
			SeismicGraph graph = new SeismicGraph();
			for(int j = 0; j < h; j++) {
				for(int i = 0; i < w; i++) {
					int id = graph.newDevice();
					assert(j*w+i == id);
				}
			}
			
			// read fanout template
			int r = in.nextInt();
			in.nextInt(); // maxFanout
			boolean[][] e = new boolean[r+1][r+1]; 
			for(int ri=0; ri<=r; ri++)
				for(int rj=0; rj<=r; rj++) {
					e[ri][rj] = in.nextInt()!=0;
				}
			
			int totalEdges = in.nextInt();
			out.printf("Nodes: %d\nEdges: %d\n", w*h, totalEdges);

			// read edge weights and create edges
			for(int sj=0; sj<h; sj++)
				for(int si=0; si<w; si++) {
					int s = sj*w+si;
					for(int rj=-r; rj<=r; rj++)
						for(int ri=-r; ri<=r; ri++) {
							if(e[Math.abs(ri)][Math.abs(rj)]) {
								int di = si+ri;
								int dj = sj+rj;
								if(di<0 || dj<0 || di>=w || dj>=h)
									continue;
								int d = dj*w+di;
								
								int x = in.nextInt();
								graph.addLabelledEdge(x, s, 0, d);
								totalEdges--;

								//printf("%d %d %.3f\n", s, d, x);
							}
						}
				}

			in.close();
			assert(totalEdges==0);
			
			int rootNode = 2*w+w/2; // TODO: source id from arg
			for(int j = 0; j < h; j++) {
				for(int i = 0; i < w; i++) {
					int node = j*w+i;
					SeismicState dev = graph.devices.get(node).s;
					dev.node = node;
					dev.isSource = (node==rootNode);
					dev.parent = -1;
					dev.dist = dev.isSource ? 0 : -1;
				}
			}

			out.printf("\nStarted\n");
			long startCompute, finishCompute;
			startCompute = currentTimeMillis();
			graph.go();
			finishCompute = currentTimeMillis();
			
			long sum = 0;
			// Accumulate sum at each device
			for(int i = 0; i < graph.hostMessages.size(); i++) {
				SeismicMessage msg = graph.hostMessages.get(i).msg;
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
		catch(Exception e) {
			e.printStackTrace();
			exit(1);
		}
	}

}
