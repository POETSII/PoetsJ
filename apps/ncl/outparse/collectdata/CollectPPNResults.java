package ncl.outparse.collectdata;

import java.io.File;

import com.xrbpowered.jdiagram.data.Data;
import com.xrbpowered.jdiagram.data.Filter;
import com.xrbpowered.jdiagram.data.Formula;
import com.xrbpowered.jdiagram.data.Join;

import ncl.outparse.Utils;
import ncl.outparse.OutParse;
import ncl.outparse.OutParse.Lookup;

public class CollectPPNResults {

	private static final String pathBase = Utils.readFile(new File("poets_data_path.txt"))+"/21jul/res/";
	private static final String[] paths = {"0730", "0723", "0722", "0712"};
	private static final String statFmt = "%s_out_stats_%d.txt";
	private static final String outFmt = "%s_out_%d.txt";
	private static final String[] modes = {"int_pipe2", "int_dphil2", "pipe2", "dphil2"};
	private static final double[] corrFactors = {3.5851, 3.4284, 3.5851, 3.4284};
	private static final int[] pPerDev = {1012, 1000, 1012, 1000};
	private static final int[] tPerDev = {460, 600, 460, 600};
	private static final int[] boxes = {1, 2, 4, 8};
	
	public static void main(String[] args) {
		Data data = new Data("boxes", "devs", "ppdev", "tpdev", "m", "time", "corr");
		Data outData = new Data("boxes", "devs", "m", "fired");
		for(int m=0; m<modes.length; m++) {
			String mode = modes[m];
			for(int b : boxes) {
				for(String p : paths) {
					String path = pathBase+p;
					String inData = Utils.readFile(new File(path, String.format(statFmt, mode, b)));
					String inOutData = Utils.readFile(new File(path, String.format(outFmt, mode, b)));
					
					if(inData!=null && inOutData!=null) {
						//System.err.printf("%s\\%s\n", path, String.format(outFmt, mode, b));
						
						new OutParse().setSection(new Lookup("Next: (\\d+)", "devs"))
							.set("boxes", b)
							.set("m", mode)
							.set("corr", corrFactors[m])
							.set("ppdev", pPerDev[m])
							.set("tpdev", tPerDev[m])
							.add(new Lookup("Time \\(s\\):  ([\\d\\.]+)", "time"))
							.parseSections(data, inData);
						//System.err.printf("%d .. %d\n", Fold.minInt(data, "devs"), Fold.maxInt(data, "devs"));
						//data.clear();
					
						new OutParse().setSection(new Lookup("Next: (\\d+)", "devs"))
							.set("boxes", b)
							.set("m", mode)
							.add(new Lookup("Total transitions fired: (\\d+)", "fired"))
							.parseSections(outData, inOutData);
						//System.err.printf("%d .. %d\n", Fold.minInt(outData, "devs"), Fold.maxInt(outData, "devs"));
						//outData.clear();
					}
				}
			}
			//System.err.println("-----------");
		}
		data.addCol("corrtime", Formula.tryOrNull(true, Formula.format("%.6f", Formula.ratio(Formula.getNum("time"), Formula.getNum("corr")))));
		data.addCol("places", Formula.tryOrNull(true, Formula.format("%.0f", Formula.prod(Formula.getNum("devs"), Formula.getNum("ppdev")))));
		data.addCol("trans", Formula.tryOrNull(true, Formula.format("%.0f", Formula.prod(Formula.getNum("devs"), Formula.getNum("tpdev")))));
		data.sort(Formula.get("m"), Formula.getInt("boxes"), Formula.getInt("devs"));
		Join.left.join(data, outData, new String[] {"boxes", "devs", "m"}, "", "t")
			.copyCols(new String[] {"boxes", "devs", "m", "places", "trans", "time", "corrtime", "tfired"})
			.filter(Filter.notNull())
			//.filter(Filter.filter("devs", "6000"))
			//.filter(Filter.filter("boxes", "1"))
			.print(System.out, true);
	}

}
