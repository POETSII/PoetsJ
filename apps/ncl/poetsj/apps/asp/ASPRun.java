package ncl.poetsj.apps.asp;

import static java.lang.System.*;
import static ncl.poetsj.apps.asp.ASPDevice.*;

import java.util.ArrayList;

import ncl.poetsj.EdgeList;
import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.asp.ASPDevice.ASPGraph;
import ncl.poetsj.apps.asp.ASPDevice.ASPState;

public class ASPRun {

	public static void main(String[] args) {
		if(args.length!=1) {
			out.printf("Specify edges file\n");
			exit(1);
		}
		
		long startAll = currentTimeMillis();
		
		out.printf("Loading...\n");
		EdgeList net = new EdgeList().read(args[0]);
		System.out.printf("Max fanout: %d\n", net.maxFanout);
		
		// Check that parameters make sense
		int numTiles = (net.numNodes+NROOTS-1)/NROOTS;
		int totalNodes = numTiles * net.numNodes;
		out.printf("Sources per tile: %d\nTiles: %d\nTotal nodes: %d\n", NROOTS, numTiles, totalNodes);
		assert(net.numNodes%NROOTS == 0);
		
		// Create POETS graph
		out.printf("Creating graph...\n");
		ASPGraph graph = new ASPGraph();
		
		// Create nodes in POETS graph
		for(int t = 0; t < numTiles; t++) {
			for(int i = 0; i < net.numNodes; i++) {
				int id = graph.newDevice();
				assert (t * net.numNodes + i == id);
			}
		}

		// Create connections in POETS graph
		for(int t = 0; t < numTiles; t++) {
			int tileOffs = t * net.numNodes;
			for(int i = 0; i < net.numNodes; i++) {
				ArrayList<Integer> edges = net.neighbours.get(i);
				int numNeighbours = edges.size();
				for(int j = 0; j < numNeighbours; j++)
					graph.addEdge(i + tileOffs, 0, edges.get(j) + tileOffs);
			}
		}
		
		for(int t = 0; t < numTiles; t++) {
			for(int i = 0; i < net.numNodes; i++) {
				int id = t * net.numNodes + i;
				ASPState dev = graph.devices.get(id).s;
				dev.globalId = id;
				dev.rootIdx = -1;
			}
		}

		if(net.numNodes % NROOTS > 0) {
			// TODO: Fill remainder tile
		}

		// Initialise sources
		int totalSources = 0;
		for(int t = 0; t < numTiles; t++) {
			int tileOffs = t * net.numNodes;
			int baseSrc = t * NROOTS + tileOffs;
			for(int i = 0; (i < NROOTS) && (baseSrc + i < totalNodes); i++) {
				ASPState dev = graph.devices.get(baseSrc + i).s;
				dev.rootIdx = i;
				totalSources++;
			}
		}
		assert (totalSources == net.numNodes);
		
		out.printf("\nStarted\n");
		long startCompute, finishCompute;
		startCompute = currentTimeMillis();
		graph.go();
		finishCompute = currentTimeMillis();

		// Sum of all shortest paths
		long sum = 0;
		// Accumulate sum at each device
		for(int i = 0; i < graph.hostMessages.size(); i++) {
			sum += graph.hostMessages.get(i).msg.hops;
		}

		// Emit sum
		out.printf("Sum = %d\n", sum);
		out.printf("ASP = %.4f\n", (double) sum / (double) net.numNodes / (double) (net.numNodes - 1));

		double duration;
		duration = (finishCompute - startCompute) / 1000.0;
		out.printf("Time (compute) = %.3f\n", duration);
		long finishAll = currentTimeMillis();
		duration = (finishAll - startAll) / 1000.0;
		out.printf("Time (all) = %.3f\n", duration);
		
		out.printf("Total messages: %d\nTotal steps: %d\n", PDevice.totalMessageCount, PGraph.totalSteps);
	}
}
