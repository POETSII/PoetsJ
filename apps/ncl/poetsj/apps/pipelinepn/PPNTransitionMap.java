package ncl.poetsj.apps.pipelinepn;

import static ncl.poetsj.apps.pipelinepn.PPNDevice.localPlaces;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.outPlaces;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.tmap;
import static ncl.poetsj.apps.pipelinepn.PPNDevice.transitions;

import ncl.poetsj.apps.pipelinepn.PPNDevice.Transition;

public abstract class PPNTransitionMap {

	private static int offs;
	
	private static int pipe(int i) {
		tmap[offs++] = new Transition(1, i, i-2);
		tmap[offs++] = new Transition(1, i+1, i-1);
		return i+2;
	}
	
	private static int sync(int i) {
		tmap[offs++] = new Transition(2, i, i-2, i-1);
		tmap[offs++] = new Transition(3, i, i+1, i+2);
		return i+3;
	}
	
	public static void create(int steps) {
		localPlaces = 9*steps;
		outPlaces = 4;
		transitions = 8*steps + 2;

		// print('#define MAX_PLACES %d' % (1<<(localPlaces+4).bit_length()))
		// print('#define MAX_TRANSITIONS %d' % (1<<(transitions+1).bit_length()))
		// print('#define MAX_OUTMAP 4\n')

		tmap = new Transition[transitions+1];
		offs = 0;
		tmap[offs++] = new Transition(1, 0, localPlaces+2);
		tmap[offs++] = new Transition(1, 1, localPlaces+3);
		int i = 2;

		for(int s=0; s<steps; s++)
			i = pipe(pipe(sync(pipe(i))));

		tmap[offs++] = new Transition(0);
		
		if(PPNDevice.logMessages)
			System.out.printf("localPlaces: %d\ntransitions: %d\n", localPlaces, transitions);
	}

}
