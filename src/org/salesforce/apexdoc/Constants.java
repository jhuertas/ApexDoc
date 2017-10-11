package org.salesforce.apexdoc;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class Constants {
        
    public static final String HEADER_OPEN = "<html>\n" +
//        "<script type='text/javascript' src='jquery-1.11.1.js'></script>" +
//        "<script type='text/javascript' src='CollapsibleList.js'></script>" +                                                                
//        "<script type='text/javascript' src='ApexDoc.js'></script>" +                                                                
//        "<link rel='stylesheet' type='text/css' href='ApexDoc.css' /> " + 
//"</head>\n" +        
        "<body>" +
		"<style>\n" + fileAsString("ApexDoc.css") + "\n</style>\n";
    
    public static final String HEADER_CLOSE =                                                                               
   
        "";
    
    public static final String FOOTER = "" + 
       "\n</body>\n</html>";
    
    public static final String ROOT_DIRECTORY = "ApexDocumentation";
    public static final String DEFAULT_HOME_CONTENTS = "<h1>zAgileConnect API</h2>";
    public static final String PROJECT_DETAIL = 
        "";     
    
    public static String getHeader(String projectDetail) {
        String header;
        if (projectDetail != null && projectDetail.trim().length() > 0) {
            header = Constants.HEADER_OPEN + projectDetail;
        } else {
            header = Constants.HEADER_OPEN + Constants.PROJECT_DETAIL + Constants.HEADER_CLOSE;
        }
        return header;
    }
    
    private static String fileAsString(String source){
    	try{
			InputStream is = Constants.class.getResourceAsStream(source);
			// InputStreamReader isr = new InputStreamReader(is);
			// BufferedReader reader = new BufferedReader(isr);

			final int bufferSize = 4096;
			final char[] buffer = new char[bufferSize];
			final StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(is, "UTF-8");
			for (;;) {
				int rsz = in.read(buffer, 0, buffer.length);
				if (rsz < 0)
					break;
				out.append(buffer, 0, rsz);
			}
			is.close();
			return out.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
    }
                
}