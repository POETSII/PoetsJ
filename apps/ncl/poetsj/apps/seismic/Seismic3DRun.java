package ncl.poetsj.apps.seismic;

import static java.lang.System.*;

import java.io.File;
import java.util.Scanner;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicGraph;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicMessage;
import ncl.poetsj.apps.seismic.SeismicDevice.SeismicState;

public class Seismic3DRun {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try {
			if(args.length!=1) {
				out.printf("Specify input file\n");
				exit(1);
			}
			
			long startAll = currentTimeMillis();
		
			// Read input
			Scanner in = new Scanner(new File(args[0]));
			
			if(!in.next().equals("T3.2"))
				throw new RuntimeException();
			
			int w = in.nextInt();
			int l = in.nextInt();
			int h = in.nextInt();
			in.nextInt(); // step
			
			// create graph
			out.printf("Loading...\n");
			SeismicGraph graph = new SeismicGraph();
			int totalNodes = 0;
			for(int k = 0; k < h; k++)
				for(int j = 0; j < l; j++)
					for(int i = 0; i < w; i++) {
						int id = graph.newDevice();
						assert((k*l+j)*w+i == id);
						totalNodes++;
					}
			
			// read fanout template
			int r = in.nextInt();
			in.nextInt(); // maxFanout
			boolean[][][] e = new boolean[r+1][r+1][r+1]; 
			for(int rk=0; rk<=r; rk++)
				for(int rj=0; rj<=r; rj++)
					for(int ri=0; ri<=r; ri++) {
						e[rk][rj][ri] = in.nextInt()!=0;
					}
			
			if(!in.next().equals("BEGIN"))
				throw new RuntimeException();
			
			// read edge weights and create edges
			int totalEdges = 0;
			for(int sk=0; sk<h; sk++)
				for(int sj=0; sj<l; sj++)
					for(int si=0; si<w; si++) {
						int s = (sk*l+sj)*w+si;
						for(int rk=-r; rk<=r; rk++)
							for(int rj=-r; rj<=r; rj++)
								for(int ri=-r; ri<=r; ri++) {
									if(e[Math.abs(rk)][Math.abs(rj)][Math.abs(ri)]) {
										int di = si+ri;
										int dj = sj+rj;
										int dk = sk+rk;
										if(di<0 || dj<0 || dk<0)
											continue;
										int d = (dk*l+dj)*w+di;
										if(s<d) {
											int x = in.nextInt();
											if(di<w && dj<l && dk<h) {
												graph.addLabelledEdge(x, s, 0, d);
												graph.addLabelledEdge(x, d, 0, s);
												totalEdges++;
											}
										}
									}
								}
					}

			if(!in.next().equals("END"))
				throw new RuntimeException();
			
			in.close();
			
			out.printf("%d nodes, %d edges\n", totalNodes, totalEdges);
			
			int rootNode = 2*w+w/2; // TODO: source id from arg
			for(int k = 0; k < h; k++)
				for(int j = 0; j < l; j++)
					for(int i = 0; i < w; i++) {
						int node = (k*l+j)*w+i;
						SeismicState dev = graph.devices.get(node).s;
						dev.node = node;
						dev.isSource = (node==rootNode);
						dev.parent = -1;
						dev.dist = dev.isSource ? 0 : -1;
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
