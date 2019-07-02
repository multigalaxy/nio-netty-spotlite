package icblive.chatserver.utils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;


public class UtilFunc {

	public static String getStackTrace(final Throwable throwable) {
		// 这个不work 
	     final StringWriter sw = new StringWriter();
	     final PrintWriter pw = new PrintWriter(sw, true);
	     throwable.printStackTrace(pw);
	     return sw.getBuffer().toString();
	}
	public static Set<String> getFileLines( String filename ){
		Set<String> tmpwords = new HashSet<String>() ;
		FileReader reader;
		try {
			reader = new FileReader( filename );
		
			  BufferedReader br = new BufferedReader(reader);
			  String s1 = null  ;
			  while((s1 = br.readLine()) != null) {
				  tmpwords.add(s1) ;
			  }
			 br.close();
			 reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace() ;
		}
		return tmpwords;
	}

	public static int parseInt(String str, int defaultInt) {
		int num = defaultInt;
		if (!isEmpty(str)) {
			try {
				num = Integer.parseInt(str.trim());
			} catch (NumberFormatException nfe) {
			} catch (Exception e) {
			}
		}
		return num;
	}

	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0 || str.trim().length() == 0;
	}
}
