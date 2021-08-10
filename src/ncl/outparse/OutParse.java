package ncl.outparse;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xrbpowered.jdiagram.data.Data;
import com.xrbpowered.jdiagram.data.Data.Row;

public class OutParse {

	public static class Lookup {
		public Pattern regex;
		public int[] groups;
		public String[] headers;
		
		public Lookup(String regex) {
			this.regex = Pattern.compile(regex, Pattern.MULTILINE+Pattern.DOTALL);
			this.groups = null;
			this.headers = null;
		}
		
		public Lookup(String regex, int[] groups, String[] headers) {
			this(regex);
			if(groups.length!=headers.length)
				throw new InvalidParameterException();
			this.groups = groups;
			this.headers = headers;
		}
		
		public Lookup(String regex, int group, String header) {
			this(regex, new int[] {group}, new String[] {header});
		}
		
		public Lookup(String regex, String header) {
			this(regex, 1, header);
		}
		
		public Matcher matcher(String s) {
			return regex.matcher(s);
		}
		
		public void assignAll(Row row, Matcher m) {
			if(groups==null)
				return;
			for(int i=0; i<groups.length; i++) {
				row.set(headers[i], m.group(groups[i]));
			}
		}
		
		public boolean findAll(Row row, String s) {
			Matcher m = matcher(s);
			if(m.find()) {
				assignAll(row, m);
				return true;
			}
			else
				return false;
		}
	}
	
	public static class Const {
		public String header;
		public Object value;
		
		public Const(String header, Object value) {
			this.header = header;
			this.value = value;
		}
		
		public void assign(Row row) {
			row.set(header, value);
		}
	}
	
	public Lookup sectionLookup = null;
	public ArrayList<Lookup> valueLookups = new ArrayList<>();
	public ArrayList<Const> constants = new ArrayList<>();
	
	public OutParse setSection(Lookup section) {
		this.sectionLookup = section;
		return this;
	}
	
	public OutParse add(Lookup val) {
		valueLookups.add(val);
		return this;
	}
	
	public OutParse set(String header, Object value) {
		constants.add(new Const(header, value));
		return this;
	}
	
	public void findAllValues(Row row, String s) {
		for(Const c : constants) {
			c.assign(row);
		}
		for(Lookup val : valueLookups) {
			val.findAll(row, s);
		}
	}
	
	public void parseSections(Data data, String s) {
		if(sectionLookup==null) {
			Row row = data.addRow();
			findAllValues(row, s);
		}
		else {
			Matcher msec = sectionLookup.matcher(s);
			int index = 0;
			int len = s.length();
			while(msec.find(index)) {
				Row row = data.addRow();
				sectionLookup.assignAll(row, msec);
				
				int start = msec.end();
				int end = len;
				if(msec.find(start))
					end = msec.start();
				
				String sec = s.substring(start, end);
				findAllValues(row, sec);
				index = end;
			}
		}
	}
	
}
