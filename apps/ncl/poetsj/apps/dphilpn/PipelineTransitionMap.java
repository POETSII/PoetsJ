package ncl.poetsj.apps.dphilpn;

import static ncl.poetsj.apps.dphilpn.DPhilDevice.localPlaces;
import static ncl.poetsj.apps.dphilpn.DPhilDevice.outPlaces;
import static ncl.poetsj.apps.dphilpn.DPhilDevice.tmap;
import static ncl.poetsj.apps.dphilpn.DPhilDevice.transitions;
import static ncl.poetsj.apps.dphilpn.DPhilRun.initPlaces;
import static ncl.poetsj.apps.dphilpn.DPhilRun.tOutMap;
import static ncl.poetsj.apps.dphilpn.DPhilRun.tileInitPlaces;

import ncl.poetsj.apps.dphilpn.DPhilDevice.OutMap;
import ncl.poetsj.apps.dphilpn.DPhilDevice.Transition;

public abstract class PipelineTransitionMap {

	public static void create(int steps) {
		// Places:
		// 0*: p0
		// 1: p1
		// 2*: p2
		// 3: p3
		// 4: p4
		// 5*: p5
		// 6: p6
		// 7*: p7
		// 8*: p8
		// 9: p9
		// 10*: p10
		// 11: p12
		// 12: p11
		// 13*: p14
		// 14*: p13
		// 15*: p16
		// 16: p15
		// 17: p18
		// 18: p17
		// 19*: p19
		// 20*: p21
		// 21: p20
		// 22: prev_p1 (out)
		// 23: prev_p3 (out)
		// 24: next_p0 (out)
		// 25: next_p2 (out)
		localPlaces = 44;
		outPlaces = 4;

		// Initial marking: 
		initPlaces = new int[] {1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0};
		tileInitPlaces = 22;

		// Output mapping:
		int offs = 0;
		tOutMap = new OutMap[outPlaces];
		tOutMap[offs++] = new OutMap(-1, 23);
		tOutMap[offs++] = new OutMap(-1, 25);
		tOutMap[offs++] = new OutMap(1, 0);
		tOutMap[offs++] = new OutMap(1, 2);

		// Transitions:
		offs = 0;
		transitions = 20; // (10 aligned to 1) x 2 steps
		tmap = new Transition[transitions];

		// {0xoi, inp-s, outp-s}
		tmap[offs++] = new Transition(0x33, 8, 10, 14, 12, 11, 9); // 0: t4
		tmap[offs++] = new Transition(0x33, 11, 16, 18, 14, 13, 15); // 1: t5
		tmap[offs++] = new Transition(0x22, 13, 19, 16, 17); // 2: t6
		tmap[offs++] = new Transition(0x22, 17, 1, 22 /* next */ , 19); // 3: t7
		tmap[offs++] = new Transition(0x22, 15, 20, 18, 21); // 4: t8
		tmap[offs++] = new Transition(0x22, 21, 3, 24 /* next */ , 20); // 5: t9
		tmap[offs++] = new Transition(0x22, 0, 5, 4, 44 /* out */ ); // 6: t0
		tmap[offs++] = new Transition(0x22, 4, 9, 5, 8); // 7: t1
		tmap[offs++] = new Transition(0x22, 2, 7, 6, 45 /* out */ ); // 8: t2
		tmap[offs++] = new Transition(0x22, 6, 12, 10, 7); // 9: t3
		tmap[offs++] = new Transition(0x33, 30, 32, 36, 34, 33, 31); // 0: t4
		tmap[offs++] = new Transition(0x33, 33, 38, 40, 36, 35, 37); // 1: t5
		tmap[offs++] = new Transition(0x22, 35, 41, 38, 39); // 2: t6
		tmap[offs++] = new Transition(0x22, 39, 23, 46 /* out */ , 41); // 3: t7
		tmap[offs++] = new Transition(0x22, 37, 42, 40, 43); // 4: t8
		tmap[offs++] = new Transition(0x22, 43, 25, 47 /* out */ , 42); // 5: t9
		tmap[offs++] = new Transition(0x22, 22, 27, 26, 1 /* prev */ ); // 6: t0
		tmap[offs++] = new Transition(0x22, 26, 31, 27, 30); // 7: t1
		tmap[offs++] = new Transition(0x22, 24, 29, 28, 3 /* prev */ ); // 8: t2
		tmap[offs++] = new Transition(0x22, 28, 34, 32, 29); // 9: t3
	}
	
}
