/*  Vertretungsplan - Android-App für Vertretungspläne von Schulen
    Copyright (C) 2014  Johan v. Forstner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see [http://www.gnu.org/licenses/]. */

package com.johan.vertretungsplan.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;

/**
 * Parser für Untis-Vertretungspläne mit dem Monitor-Stundenplan-Layout
 * Beispiel: Lornsenschule Schleswig http://vertretung.lornsenschule.de/schueler/subst_001.htm
 * Funktioniert mit vielen anderen Schulen mit unterschiedlichen Layouts.
 */
public class UntisMonitorParser extends UntisCommonParser {
	
	public UntisMonitorParser(Schule schule) {
		super(schule);
	}
	
	public Vertretungsplan getVertretungsplan() throws IOException, JSONException {	
		handleLogin();
		
		JSONArray urls = schule.getData().getJSONArray("urls");
		String encoding = schule.getData().getString("encoding");
		List<Document> docs = new ArrayList<Document>();
		
		for(int i = 0; i < urls.length(); i++) {
			JSONObject url = urls.getJSONObject(i);
			loadUrl(url.getString("url"), encoding, url.getBoolean("following"), docs);
		}
		
		LinkedHashMap<String, VertretungsplanTag> tage = new LinkedHashMap<String, VertretungsplanTag>();
		for(Document doc:docs) {
			if (doc.title().contains("Untis")) {
				VertretungsplanTag tag = parseVertretungsplanTag(doc, schule.getData());
				if(!tage.containsKey(tag.getDatum())) {
					tage.put(tag.getDatum(), tag);
				} else {
					VertretungsplanTag tagToMerge = tage.get(tag.getDatum());
					tagToMerge.merge(tag);
					tage.put(tag.getDatum(), tagToMerge);
				}
			} else {
				//Fehler
			}
		}
		Vertretungsplan v = new Vertretungsplan();
		v.setTage(new ArrayList<VertretungsplanTag>(tage.values()));
		
		return v;
	}
	
	private void handleLogin() throws JSONException, IOException {
		if (!schule.getData().has("login"))
			return;
		
		String url = schule.getData().getJSONObject("login").getString("url");
		JSONObject data = schule.getData().getJSONObject("login").getJSONObject("data");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		for (String name:JSONObject.getNames(data)) {
			nvps.add(new BasicNameValuePair(name, data.getString(name)));
		}
		httpPost(url, schule.getData().getString("encoding"), nvps);
	}

	private void loadUrl(String url, String encoding, boolean following, List<Document> docs, String startUrl) throws IOException {
		String html = httpGet(url, encoding).replace("&nbsp;", "");
		Document doc = Jsoup.parse(html);
		docs.add(doc);
		if(following
				&& doc.select("meta[http-equiv=refresh]").size() > 0) {
			Element meta = doc.select("meta[http-equiv=refresh]").first();
			String attr = meta.attr("content").toLowerCase();
			String redirectUrl = url.substring(0, url.lastIndexOf("/") + 1) + attr.substring(attr.indexOf("url=") + 4);
			if (!redirectUrl.equals(startUrl))
				loadUrl(redirectUrl, encoding, true, docs, startUrl);
		}
	}
	
	private void loadUrl(String url, String encoding, boolean following, List<Document> docs) throws IOException {
		loadUrl(url, encoding, following, docs, url);
	}
	
	protected VertretungsplanTag parseVertretungsplanTag(Document doc, JSONObject data) throws JSONException {
 		VertretungsplanTag tag = new VertretungsplanTag();
		tag.setDatum(doc.select(".mon_title").first().text().replaceAll(" \\(Seite \\d / \\d\\)", ""));	
		if(data.optBoolean("stand_links", false)) {
			tag.setStand(doc.select("body").html().substring(0, doc.select("body").html().indexOf("<p>")-1));
		} else {
			Element stand = doc.select("table.mon_head td[align=right] p").first();
			String info = stand.text();
			tag.setStand(info.substring(info.indexOf("Stand:")));
		}
 		
 		//NACHRICHTEN
		if(doc.select("table.info").size() > 0)
			parseNachrichten(doc.select("table.info").first(), data, tag);
 		
 		//VERTRETUNGSPLAN
 		parseVertretungsplanTable(doc, data, tag);	 		
 		
 		return tag;
	}
	
	public List<String> getAllClasses() throws JSONException {
		JSONArray classesJson = schule.getData().getJSONArray("classes");
		List<String> classes = new ArrayList<String>();
		for(int i = 0; i < classesJson.length(); i++) {
			classes.add(classesJson.getString(i));
		}
		return classes;
	}
}
