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
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
@SuppressWarnings("serial")
public class GetLogsServlet extends HttpServlet {
	private static final int PAGE_SIZE = 10;
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	

		int page = req.getParameter("page") != null ?
				Integer.parseInt(req.getParameter("page")) : 0;
		
		boolean errorsOnly = req.getParameter("errorsOnly") != null ?
				Boolean.parseBoolean(req.getParameter("errorsOnly")) : false;
		
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");	
		
		DBCollection coll = db.getCollection("logs");	
		
		DBCursor logs;
		if(errorsOnly) {
			DBObject query = new BasicDBObject("hasErrors", true);
			logs = coll.find(query);
		} else {
			logs = coll.find();
		}
		
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		
		DBObject order = new BasicDBObject("_id", -1);
		logs.sort(order);
		logs.skip(PAGE_SIZE * page);
		int i = 0;
		
		resp.getWriter().println("[");
		while(logs.hasNext() && i < PAGE_SIZE) {
			DBObject log = logs.next();
			resp.getWriter().println(JSON.serialize(log) + ((logs.hasNext() &&
					i < (PAGE_SIZE - 1)) ? "," : ""));
			i++;
		}	
		resp.getWriter().println("]");
		resp.getWriter().close();
		
		logs.close();

	}
}
