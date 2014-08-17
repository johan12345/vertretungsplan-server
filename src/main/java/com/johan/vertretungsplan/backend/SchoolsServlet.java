package com.johan.vertretungsplan.backend;

import java.io.BufferedReader;
import java.io.IOException;

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
public class SchoolsServlet extends HttpServlet {
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {	
		String schoolId = req.getParameter("id");
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("school_info");
		BasicDBObject query = new BasicDBObject("_id", schoolId);
		DBCursor cursor = coll.find(query);
		if(cursor.size() == 1) {
			DBObject school = cursor.next();
			String json = (String) school.get("json");
			
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().print(json);
			resp.getWriter().close();
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {	
		boolean overwrite = "yes".equals(req.getParameter("overwrite"));
		
		StringBuilder sb = new StringBuilder();
	    BufferedReader reader = req.getReader();
	    try {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line).append('\n');
	        }
	    } finally {
	        reader.close();
	    }
		String json = sb.toString();
		String schoolId = req.getParameter("id");		
		
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("school_info");
		BasicDBObject school = new BasicDBObject("_id", schoolId);
		school.put("json", json);
		if (overwrite) coll.save(school); else coll.insert(school);
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	
	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {	
		String schoolId = req.getParameter("id");
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("school_info");
		BasicDBObject query = new BasicDBObject("_id", schoolId);
		DBCursor cursor = coll.find(query);
		if(cursor.size() == 1) {
			DBObject school = cursor.next();
			coll.remove(school);
			
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
