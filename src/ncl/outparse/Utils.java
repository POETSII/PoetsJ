package ncl.outparse;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {

	public static String readFile(File file) {
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			byte bytes[] = new byte[in.available()];
			in.readFully(bytes);
			in.close();
			return new String(bytes);
		}
		catch(IOException e) {
			//e.printStackTrace();
			return null;
		}
	}
	
}
