package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.johan.vertretungsplan.objects.Schule;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
@SuppressWarnings("serial")
public class GetSchoolsServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException {	
		JSONArray allSchools = new JSONArray();
		for (Schule school:getSchools()) {
			try {			
				JSONObject json = new JSONObject();
				json.put("id", school.getId());
				json.put("name", school.getName());
				json.put("city", school.getCity());
				if(school.getGeo() != null) {
					JSONArray geo = new JSONArray();
					geo.put(school.getGeo()[0]);
					geo.put(school.getGeo()[1]);
					json.put("geo", geo);
				}
				
				MongoClient client = DBManager.getInstance();
				DB db = client.getDB("vertretungsplan");
				
				DBCollection coll = db.getCollection("registrations");
				BasicDBObject sub = new BasicDBObject("schoolId", school.getId());
				DBCursor cursor = coll.find(sub);
				json.put("user_count", cursor.count());
				
				allSchools.put(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
				
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().print(allSchools.toString());
		resp.getWriter().close();
	}
	
	public static List<Schule> getSchools() throws FileNotFoundException, IOException {
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("school_info");
		
		List<Schule> schools = new ArrayList<Schule>();
		for(DBObject obj:coll.find()) {	
			String schoolId = (String) obj.get("_id");
			String jsonString = (String) obj.get("json");
			schools.add(Schule.fromJSON(schoolId, new JSONObject(jsonString)));
		}
		return schools;
	}
}
