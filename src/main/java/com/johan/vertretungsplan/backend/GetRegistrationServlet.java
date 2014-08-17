package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
@SuppressWarnings("serial")
public class GetRegistrationServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	
		String subId = req.getParameter("subId");
		
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("registrations");
		BasicDBObject sub = new BasicDBObject("_id", subId);
		DBCursor cursor = coll.find(sub);
		if(cursor.hasNext()) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().print(cursor.next());
			resp.getWriter().close();
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
