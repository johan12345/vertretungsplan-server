package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@SuppressWarnings("serial")
public class WebsiteServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	
		String schoolId = req.getPathInfo().replace("/", "");
		DBCollection coll = DBManager.getInstance().getDB("vertretungsplan").getCollection("school_info");
		DBObject school = coll.findOne(new BasicDBObject("_id", schoolId));
		if (school != null) {
			JSONObject json = new JSONObject((String) school.get("json"));
			if (json.has("website")) {
				resp.sendRedirect(json.getString("website"));
				return;
			} else if (json.has("api") && json.getString("api").equals("dsbmobile")) {
				resp.sendRedirect("https://mobile.dsbcontrol.de/DSBmobilePage.aspx");
				return;
			} else if (json.has("api") && json.getString("api").equals("untis-info")) {
				resp.sendRedirect(json.getJSONObject("data").getString("baseurl") + "/default.htm");
				return;
			}
		}
		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);		
	}
}
