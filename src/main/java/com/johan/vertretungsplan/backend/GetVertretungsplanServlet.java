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
public class GetVertretungsplanServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	
		int version = Integer.parseInt(req.getParameter("v"));
		if(version >= Settings.MIN_SUPPORTED_VERSION) {
			String schoolId = req.getParameter("school");
			
			MongoClient client = DBManager.getInstance();
			DB db = client.getDB("vertretungsplan");			
			
			DBCollection coll = db.getCollection("schools");		
			BasicDBObject query = new BasicDBObject("_id", schoolId);
			DBObject school = coll.findOne(query);
			
			if(school != null) {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().print((String) school.get("vertretungsplan"));
				resp.getWriter().close();
			} else {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} else {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().print("please update");
			resp.getWriter().close();
		}
	}
}
