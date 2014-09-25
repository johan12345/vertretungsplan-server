package com.johan.vertretungsplan.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.johan.vertretungsplan.backend.DBManager;
import com.johan.vertretungsplan.objects.Schule;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class LoginHandler {
	private Schule schule;
	
	public LoginHandler(Schule schule) {
		this.schule = schule;
	}
	
	public void handleLogin(Executor executor, CookieStore cookieStore, String login, String password) throws JSONException, IOException {
		if (!schule.getData().has("login"))
			return;
		
		String type = schule.getData().getJSONObject("login").optString("type", "post");
		if (type.equals("post")) {
			List<Cookie> cookies = getCookies();
			if (cookies != null) {
				for (Cookie cookie:cookies)
					cookieStore.addCookie(cookie);
			} else {			
				String url = schule.getData().getJSONObject("login").getString("url");
				JSONObject data = schule.getData().getJSONObject("login").getJSONObject("data");
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				for (String name:JSONObject.getNames(data)) {
					String value = data.getString(name);
					if (value.equals("_login"))
						value = login;
					else if (value.equals("_password"))
						value = password;
					nvps.add(new BasicNameValuePair(name, value));
				}
				executor.clearCookies();
				executor.execute(Request.Post(url)
					.bodyForm(nvps)).returnResponse();
				saveCookies(cookieStore);			
			}			
		} else if (type.equals("basic")) {
			executor.auth(login, password);
		} else if (type.equals("fixed")) {
			String loginFixed = schule.getData().getJSONObject("login").getString("login");
			String passwordFixed = schule.getData().getJSONObject("login").getString("password");
			if (!loginFixed.equals(login) || !passwordFixed.equals("password"))
				throw new IOException("wrong login/password");
		}
	}
	
	private List<Cookie> getCookies() {
		DB db = DBManager.getInstance().getDB("vertretungsplan");
		DBCollection cookiesColl = db.getCollection("cookies");
		DBObject obj = cookiesColl.findOne(new BasicDBObject("_id", schule.getId()));
		if (obj != null) {
			String cookieJson = (String) obj.get("cookies");
			List<Cookie> cookies = new Gson().fromJson(cookieJson, new TypeToken<List<BasicClientCookie>>(){}.getType());	
			for (Cookie cookie:cookies) {
				if (cookie.isExpired(new Date()))
					return null;
			}
			return cookies;
		}
		return null;
	}
	
	private void saveCookies(CookieStore cookieStore) {
		DB db = DBManager.getInstance().getDB("vertretungsplan");
		DBCollection cookiesColl = db.getCollection("cookies");
		DBObject obj = new BasicDBObject("_id", schule.getId());
		
		String cookieJson = new Gson().toJson(cookieStore.getCookies());
		obj.put("cookies", cookieJson);
		cookiesColl.save(obj);
	}
}
