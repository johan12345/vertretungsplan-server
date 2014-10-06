package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
@SuppressWarnings("serial")
public class CheckHealthServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {			
		if (checkHealth()) {
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	private boolean checkHealth() {
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");	
		
		DBCollection coll = db.getCollection("logs");	
		DBCursor cursor = coll.find().sort(new BasicDBObject("date", -1));
		if (cursor.hasNext()) {
			DBObject newest = cursor.next();
			cursor.close();
			Date date = (Date) newest.get("date");
			Date now = new Date();
			long difference = now.getTime() - date.getTime();
			if (difference < 1000 * 60 * 10) {
				return true;
			}
		} else {
			cursor.close();
		}
		return false;
	}
}
