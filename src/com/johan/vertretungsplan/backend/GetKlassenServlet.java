package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
@SuppressWarnings("serial")
public class GetKlassenServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	
		String schoolId = req.getParameter("school");

		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");	
		
		DBCollection coll = db.getCollection("schools");		
		BasicDBObject query = new BasicDBObject("_id", schoolId);
		DBObject school = coll.findOne(query);
		if(school != null) {
			resp.setStatus(HttpServletResponse.SC_OK);
			String schools = (String) school.get("classes");
			resp.getWriter().print(schools);
			resp.getWriter().close();
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
