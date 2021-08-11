package ncl.outparse.collectdata;

import java.io.File;

import com.xrbpowered.jdiagram.data.Data;
import com.xrbpowered.jdiagram.data.Filter;
import com.xrbpowered.jdiagram.data.Formula;

import ncl.outparse.OutParse;
import ncl.outparse.OutParse.Lookup;
import ncl.outparse.Utils;

public class CollectCpuPNResults {

	private static final String pathBase = Utils.readFile(new File("poets_data_path.txt"))+"/21aug/cpu_res/poets_cpu";
	private static final String outFmt = "%s_out_cpu.txt";
	private static final String[] modes = {"int_pipe2", "int_dphil2", "pipe2", "dphil2"};
	private static final int[] pPerDev = {1012, 1000, 1012, 1000};
	private static final int[] tPerDev = {460, 600, 460, 600};
	
	public static void main(String[] args) {
		Data data = new Data("boxes", "devs", "ppdev", "tpdev", "m", "time", "tfired");
		for(int m=0; m<modes.length; m++) {
			String mode = modes[m];
			String inData = Utils.readFile(new File(pathBase, String.format(outFmt, mode)));
			
			if(inData!=null) {
				new OutParse().setSection(new Lookup("Next: (\\d+)", "devs"))
					.set("boxes", "cpu")
					.set("m", mode)
					.set("ppdev", pPerDev[m])
					.set("tpdev", tPerDev[m])
					.add(new Lookup("Transitions fired\\: ([\\d]+)", "tfired"))
					.add(new Lookup("Time \\(compute\\) \\= ([\\d\\.]+)", "time"))
					.parseSections(data, inData);
			}
			else {
				System.err.println("No file: "+pathBase+"/"+String.format(outFmt, mode));
			}
		}

		data.addCol("places", Formula.tryOrNull(true, Formula.format("%.0f", Formula.prod(Formula.getNum("devs"), Formula.getNum("ppdev")))));
		data.addCol("trans", Formula.tryOrNull(true, Formula.format("%.0f", Formula.prod(Formula.getNum("devs"), Formula.getNum("tpdev")))));
		data.sort(Formula.get("m"), Formula.getInt("devs"));
		data.copyCols(new String[] {"boxes", "devs", "m", "places", "trans", "time", "tfired"})
			.filter(Filter.notNull())
			//.filter(Filter.filter("devs", "6000"))
			//.filter(Filter.filter("boxes", "1"))
			.print(System.out, true);
	}

}
