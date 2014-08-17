package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.*;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
@SuppressWarnings("serial")
public class RegisterServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	
		String subId = req.getParameter("subId");
		String deviceInfo = req.getParameter("deviceInfo");
		String klasse = req.getParameter("klasse");
		String schoolId = req.getParameter("school");
		long timestamp = System.currentTimeMillis();
		
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("registrations");		
		BasicDBObject sub = new BasicDBObject("_id", subId)			
			.append("deviceInfo", deviceInfo)
			.append("type", "gcm")
			.append("klasse", klasse)
			.append("schoolId", schoolId)
			.append("timestamp", timestamp);
		coll.save(sub);
	}
}
