package com.johan.vertretungsplan.backend;

import com.mongodb.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
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
            String classes = (String) school.get("classes");
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().print(classes);
            resp.getWriter().close();
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
