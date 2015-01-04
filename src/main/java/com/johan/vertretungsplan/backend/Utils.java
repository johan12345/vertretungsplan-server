package com.johan.vertretungsplan.backend;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;

public class Utils {
	public static String getFileContent(FileInputStream fis, String encoding ) throws IOException {
		  BufferedReader br =
		           new BufferedReader( new InputStreamReader(fis, encoding ));
	      StringBuilder sb = new StringBuilder();
	      String line;
	      while(( line = br.readLine()) != null ) {
	         sb.append( line );
	         sb.append( '\n' );
	      }
	      return sb.toString();	
	}
	
	public static long getTimeInMillis() {
		return Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).getTimeInMillis();
	}
}
