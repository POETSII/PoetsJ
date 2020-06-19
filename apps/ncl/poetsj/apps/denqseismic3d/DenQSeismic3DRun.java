package ncl.poetsj.apps.denqseismic3d;

import static java.lang.System.*;
import static ncl.poetsj.apps.denqseismic3d.DenQSeismic3DDevice.*;

import java.io.DataInputStream;
import java.io.FileInputStream;

import ncl.poetsj.PDevice;
import ncl.poetsj.PGraph;
import ncl.poetsj.apps.denqseismic3d.DenQSeismic3DDevice.DenQSeismic3DGraph;
import ncl.poetsj.apps.denqseismic3d.DenQSeismic3DDevice.DenQSeismic3DMessage;
import ncl.poetsj.apps.denqseismic3d.DenQSeismic3DDevice.DenQSeismic3DState;

public class DenQSeismic3DRun {

	public static void main(String[] args) {
		try {
			int w = 0;
			int l = 0;
			int h = 0;
			if(args.length==4) {
				w = Integer.parseInt(args[0]);
				l = Integer.parseInt(args[1]);
				h = Integer.parseInt(args[2]);
			}
			if(w<1 || l<1 || h<1) {
				out.printf("Usage: w l h input\n");
				exit(1);
			}
			
			long startAll = currentTimeMillis();
		
			// Read input
			out.printf("Loading density map...\n");
			DataInputStream in = new DataInputStream(new FileInputStream(args[3]));
			int sw = in.readInt();
			int sl = in.readInt();
			int sh = in.readInt();
			byte[] bytes = new byte[sw*sl*sh];
			in.readFully(bytes);

			int[][][] den = new int[sw][sl][sh];
			int offs = 0;
			for(int x=0; x<sw; x++) {
				for(int y=0; y<sl; y++) {
					for(int z=0; z<sh; z++) {
						den[x][y][z] = (int)bytes[offs++] & 0xff; //in.readByte() & 0xff;
					}
				}
			}
			in.close();
			
			// create graph
			out.printf("Creating devices...\n");
			DenQSeismic3DGraph graph = new DenQSeismic3DGraph();
			graph.devices.ensureCapacity(w*l*h);
			for(int k=0; k<h; k++) {
				for(int j=0; j<l; j++) {
					for(int i=0; i<w; i++) {
						int id = graph.newDevice();
						assert((k*l+j)*w+i == id);
					}
				}
			}
			
			for(int k=0; k<h; k++) {
				for(int j = 0; j<h; j++) {
					for(int i = 0; i<w; i++) {
						int src = (k*l+j)*w+i;
						if(i>0) graph.addEdge(src, 0, src-1);
						if(i<w-1) graph.addEdge(src, 0, src+1);
						if(j>0) graph.addEdge(src, 0, src-w);
						if(j<l-1) graph.addEdge(src, 0, src+w);
						if(k>0) graph.addEdge(src, 0, src-w*l);
						if(k<h-1) graph.addEdge(src, 0, src+w*l);
					}
				}
			}
			
			out.printf("Nodes: %d\n", w*h*l);
			
			// read edge weights and create edges
			int rootNode = 2*w+w/2; // TODO: source id from arg
			for(int sk=0; sk<h; sk++)
				for(int sj=0; sj<l; sj++)
					for(int si=0; si<w; si++) {
						int s = (sk*l+sj)*w+si;
						DenQSeismic3DState dev = graph.devices.get(s).s;
						dev.x = si;
						dev.y = sj;
						dev.z = sk;
						dev.isSource = (s==rootNode);
						
						for(int rk=-R; rk<=R; rk++)
							for(int rj=-R; rj<=R; rj++)
								for(int ri=-R; ri<=R; ri++) {
									int i = ri+R;
									int j = rj+R;
									int k = rk+R;
									int index = DenQSeismic3DDevice.makeIndex(i, j, k);
									
									int di = (si+ri+sw)%sw;
									int dj = (sj+rj+sl)%sl;
									int dk = (sk+rk+sh)%sh;
									
									dev.changed[index] = den[di][dj][dk];
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
				DenQSeismic3DMessage msg = graph.hostMessages.get(i).msg;
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
