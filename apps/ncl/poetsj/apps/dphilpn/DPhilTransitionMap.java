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
import ncl.poetsj.apps.pipelinepn.PPNDevice;

public abstract class DPhilTransitionMap {

	private static int offs;
	
	public static void create(int steps) {
		// Places:
		// 0: ccw_before_put_left_fork
		// 1*: ccw_left_hand_empty
		// 2: before_put_right_fork
		// 3*: left_hand_empty
		// 4*: fork1_free
		// 5: ordering
		// 6: after_take_left_fork
		// 7: eating
		// 8*: thinking
		// 9: after_take_right_fork
		// 10: before_put_left_fork
		// 11*: fork2_free
		// 12*: right_hand_empty
		// 13: cw_eating
		// 14*: cw_thinking
		// 15: cw_after_take_left_fork
		// 16*: cw_right_hand_empty
		// 17: cw_ordering
		// 18: cw_before_put_right_fork
		// 19: cw_after_take_right_fork
		// 20: ccw_ordering (out)
		// 21: ccw_after_take_left_fork (out)
		// 22: cw_before_put_left_fork (out)
		localPlaces = 40;
		outPlaces = 3;

		// Initial marking: 
		initPlaces = new int[] {0, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0};
		tileInitPlaces = 20;

		// Output mapping:
		offs = 0;
		tOutMap = new OutMap[outPlaces];
		tOutMap[offs++] = new OutMap(-1, 37);
		tOutMap[offs++] = new OutMap(-1, 35);
		tOutMap[offs++] = new OutMap(1, 0);

		// Transitions:
		offs = 0;
		transitions = 24; // (12 aligned to 4) x 2 steps
		tmap = new Transition[transitions];

		// {0xoi, inp-s, outp-s}
		tmap[offs++] = new Transition(0x22, 4, 1, 41 /* out */ , 40 /* out */ ); // 0: ccw_take_left_fork *choice
		tmap[offs++] = new Transition(0x22, 4, 12, 9, 5); // 1: take_right_fork *choice
		tmap[offs++] = new Transition(0x21, 0, 1, 4); // 2: ccw_put_left_fork
		tmap[offs++] = new Transition(0x21, 10, 11, 3); // 3: put_left_fork

		tmap[offs++] = new Transition(0x31, 7, 10, 2, 8); // 4: start_thinking
		tmap[offs++] = new Transition(0x13, 9, 6, 8, 7); // 5: start_eating
		tmap[offs++] = new Transition(0x21, 2, 4, 12); // 6: put_right_fork
		tmap[offs++] = new Transition(0x31, 13, 20 /* next */ , 18, 14); // 7: cw_start_thinking

		tmap[offs++] = new Transition(0x13, 11, 3, 5, 6); // 8: take_left_fork *choice
		tmap[offs++] = new Transition(0x13, 11, 16, 17, 19); // 9: cw_take_right_fork *choice
		tmap[offs++] = new Transition(0x21, 18, 16, 11); // 10: cw_put_right_fork
		tmap[offs++] = new Transition(0x13, 14, 19, 15, 13); // 11: cw_start_eating

		tmap[offs++] = new Transition(0x22, 24, 21, 15 /* prev */ , 17 /* prev */ ); // 0: ccw_take_left_fork *choice
		tmap[offs++] = new Transition(0x22, 24, 32, 29, 25); // 1: take_right_fork *choice
		tmap[offs++] = new Transition(0x21, 20, 21, 24); // 2: ccw_put_left_fork
		tmap[offs++] = new Transition(0x21, 30, 31, 23); // 3: put_left_fork

		tmap[offs++] = new Transition(0x31, 27, 30, 22, 28); // 4: start_thinking
		tmap[offs++] = new Transition(0x13, 29, 26, 28, 27); // 5: start_eating
		tmap[offs++] = new Transition(0x21, 22, 24, 32); // 6: put_right_fork
		tmap[offs++] = new Transition(0x31, 33, 42 /* out */ , 38, 34); // 7: cw_start_thinking

		tmap[offs++] = new Transition(0x13, 31, 23, 25, 26); // 8: take_left_fork *choice
		tmap[offs++] = new Transition(0x13, 31, 36, 37, 39); // 9: cw_take_right_fork *choice
		tmap[offs++] = new Transition(0x21, 38, 36, 31); // 10: cw_put_right_fork
		tmap[offs++] = new Transition(0x13, 34, 39, 35, 33); // 11: cw_start_eating

		if(PPNDevice.logMessages)
			System.out.printf("localPlaces: %d\ntransitions: %d\n", localPlaces, transitions);
	}

}
