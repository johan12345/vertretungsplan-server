package com.johan.vertretungsplan.backend;

public class ParseThreadResult {
	Exception ex;
	String gcmResult;
	
	public ParseThreadResult(Exception ex) {
		this.ex = ex;
	}
	
	public ParseThreadResult(String gcmResult) {
		this.gcmResult = gcmResult;
	}
}
