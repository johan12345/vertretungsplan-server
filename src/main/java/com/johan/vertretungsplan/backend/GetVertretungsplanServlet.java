package com.johan.vertretungsplan.backend;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.johan.vertretungsplan.objects.Schule;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

@SuppressWarnings("serial")
public class GetVertretungsplanServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws FileNotFoundException, IOException {
		int version = Integer.parseInt(req.getParameter("v"));
		if (version >= Settings.MIN_SUPPORTED_VERSION) {
			String schoolId = req.getParameter("school");
			String regId = req.getParameter("regId");

			MongoClient client = DBManager.getInstance();
			DB db = client.getDB("vertretungsplan");

			if (regId != null) {
				DBCollection regColl = db.getCollection("registrations");
				BasicDBObject query = new BasicDBObject("_id", regId);
				DBObject reg = regColl.findOne(query);
				if (reg == null || (reg.containsField("password_invalid") && (Boolean) reg
								.get("password_invalid") == true)) {
					unauthorized(resp);
					return;
				}
				if (!schoolId.equals((String) reg.get("schoolId"))) {
					unauthorized(resp);
					return;
				}
			} else {
				DBCollection coll = db.getCollection("school_info");
				BasicDBObject query = new BasicDBObject("_id", schoolId);
				DBObject obj = coll.findOne(query);
				String jsonString = (String) obj.get("json");
				Schule school = Schule.fromJSON(schoolId, new JSONObject(
						jsonString));
				if (school.getData().has("login")) {
					unauthorized(resp);
				}
			}

			DBCollection coll = db.getCollection("schools");
			BasicDBObject query2 = new BasicDBObject("_id", schoolId);
			DBObject school = coll.findOne(query2);

			if (school != null) {
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

	private void unauthorized(HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		resp.setContentType("text/plain");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().print("unauthorized");
		resp.getWriter().close();
	}
}
