package ncl.outparse.collectdata;

import java.io.File;
import java.util.NoSuchElementException;

import com.xrbpowered.jdiagram.data.Data;
import com.xrbpowered.jdiagram.data.Filter;
import com.xrbpowered.jdiagram.data.Data.Row;

import ncl.outparse.Utils;

public class PNCalcF {

	private static final String pathBase = Utils.readFile(new File("poets_data_path.txt"))+"/21jul/res";
	private static final String[] modes = {"dphil2", "pipe2", "int_dphil2", "int_pipe2"};
	private static final int[] devs = {6000, 12000, 24000, 48000};
	private static final int[] boxes = {1, 2, 4, 8};
	
	public static void main(String[] args) {
		/*Data data = Data.read(new File(pathBase, "cpu_pn_results.csv"));
		data = data.filter(new Filter() {
			@Override
			public boolean accept(Row row) {
				int d = row.getInt("devs");
				for(int dev : devs)
					if(d==dev) return true;
				return false;
			}
		});
		data = data.copyCols(new String[] {"m", "devs", "time"});
		data.print();*/
		Data data = Data.read(new File(pathBase, "ppn_results.csv"));
		for(String m : modes)
			for(int d : devs) {
				for(int b : boxes) {
					try {
						String s = data.filter(new Filter() {
							@Override
							public boolean accept(Row row) {
								return row.getInt("devs")==d && row.getInt("boxes")==b && row.get("m").equals(m);
							}
						}).rows().iterator().next().get("corrtime");
						System.out.printf("%s\t", s);
					}
					catch (NoSuchElementException e) {
						System.out.print("\t");
					}
				}
				System.out.println();
			}
	}

}
