package com.johan.vertretungsplan.backend;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.parser.BaseParser;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

@SuppressWarnings("serial")
public class CheckLoginServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String login = req.getParameter("login");
		String password = req.getParameter("password");
		String schoolId = req.getParameter("schoolId");
		try {
			checkLogin(schoolId, login, password);
			resp.setStatus(HttpServletResponse.SC_ACCEPTED);
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			e.printStackTrace(resp.getWriter());
			resp.getWriter().close();
		}
	}

	public static Schule getSchoolById(String id) {
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");

		DBCollection coll = db.getCollection("school_info");
		DBObject query = new BasicDBObject("_id", id);
		DBObject school = coll.findOne(query);
		String json = (String) school.get("json");
		Schule schule = Schule.fromJSON(id, new JSONObject(json));
		return schule;
	}

	public static void checkLogin(String schoolId, String login, String password) throws Exception {
		Schule school = getSchoolById(schoolId);
		BaseParser parser = BaseParser.getInstance(school);
		parser.setUsername(login);
		parser.setPassword(password);
		Vertretungsplan v = parser.getVertretungsplan();
		if (v.getTage().size() == 0)
			throw new Exception("kein Vertretungsplanabruf möglich");
	}
}
