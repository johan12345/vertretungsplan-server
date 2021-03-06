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

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser für Untis-Vertretungspläne mit dem Info-Stundenplan-Layout
 * Beispiel: AKG Bensheim http://www.akg-bensheim.de/akgweb2011/content/Vertretung/default.htm
 *
 */
public class UntisInfoParser extends UntisCommonParser {
	
	private String baseUrl;
	private JSONObject data;
	private String navbarDoc;

	public UntisInfoParser(Schule schule) {
		super(schule);
		try {
			data = schule.getData();
			baseUrl = data.getString("baseurl");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private String getNavbarDoc() throws JSONException, IOException {
		if(navbarDoc == null) {
			String navbarUrl = baseUrl + "/frames/navbar.htm";
			navbarDoc = httpGet(navbarUrl, schule.getData().getString("encoding"));
		}
		return navbarDoc;
	}

	@Override
	public Vertretungsplan getVertretungsplan()
			throws IOException, JSONException {
		new LoginHandler(schule).handleLogin(executor, cookieStore, username, password);
		
		Document navbarDoc = Jsoup.parse(getNavbarDoc().replace("&nbsp;", ""));
		Element select = navbarDoc.select("select[name=week]").first();
		
		Vertretungsplan v = new Vertretungsplan();
		List<VertretungsplanTag> tage = new ArrayList<VertretungsplanTag>();
		
		String info = navbarDoc.select(".description").text();
		String stand;
		try {
			stand = info.substring(info.indexOf("Stand:"));
		} catch (Exception e) {
			stand = "";
		}
		
		for (Element option:select.children()) {
			String week = option.attr("value");
            String letter = data.optString("letter", "w");
            if (data.optBoolean("single_classes", false)) {
				int classNumber = 1;
				for (String klasse:getAllClasses()) {
					String paddedNumber = String.format("%05d", classNumber);
					String url;
					if (data.optBoolean("w_after_number", false))
                        url = baseUrl + "/" + week + "/" + letter + "/" + letter + paddedNumber +
                                ".htm";
                    else
                        url = baseUrl + "/" + letter + "/" + week + "/" + letter + paddedNumber +
                                ".htm";

                    Document doc = Jsoup.parse(httpGet(url, schule.getData().getString("encoding")));
					Elements days = doc.select("#vertretung > p > b, #vertretung > b");
					for(Element day:days) {
						VertretungsplanTag tag = getTagByDatum(tage, day.text());
						tag.setStand(stand);
						tag.setDatum(day.text());
						Element next = null;		
						if (day.parent().tagName().equals("p")) {
							next = day.parent().nextElementSibling().nextElementSibling();
						} else
							next = day.parent().select("p").first().nextElementSibling();
						if (next.className().equals("subst")) {
							//Vertretungstabelle
							if(next.text().contains("Vertretungen sind nicht freigegeben"))
								continue;
							parseVertretungsplanTable(next, data, tag);
						} else {
							//Nachrichten
							parseNachrichten(next, data, tag);
							next = next.nextElementSibling().nextElementSibling();
							parseVertretungsplanTable(next, data, tag);
						}
						writeTagByDatum(tage, tag);
					}
					
					classNumber ++;
				}
			} else {
				String url;
				if (data.optBoolean("w_after_number", false))
                    url = baseUrl + "/" + week + "/" + letter + "/" + letter + "00000.htm";
                else
                    url = baseUrl + "/" + letter + "/" + week + "/" + letter + "00000.htm";
                Document doc = Jsoup.parse(httpGet(url, schule.getData().getString("encoding")));
				Elements days = doc.select("#vertretung > p > b, #vertretung > b");
				for(Element day:days) {
					VertretungsplanTag tag = getTagByDatum(tage, day.text());
					tag.setStand(stand);
					tag.setDatum(day.text());
					Element next = null;		
					if (day.parent().tagName().equals("p")) {
						next = day.parent().nextElementSibling().nextElementSibling();
					} else
						next = day.parent().select("p").first().nextElementSibling();
					if (next.className().equals("subst")) {
						//Vertretungstabelle
						if(next.text().contains("Vertretungen sind nicht freigegeben"))
							continue;
						parseVertretungsplanTable(next, data, tag);
					} else {
						//Nachrichten
						parseNachrichten(next, data, tag);
						next = next.nextElementSibling().nextElementSibling();
						parseVertretungsplanTable(next, data, tag);
					}
					tage.add(tag);
				}
			}
			v.setTage(tage);
		}
		return v;
	}

	@Override
	public List<String> getAllClasses() throws JSONException, IOException {
		String js = getNavbarDoc();
		Pattern pattern = Pattern.compile("var classes = (\\[[^\\]]*\\]);");
		Matcher matcher = pattern.matcher(js);		
		if(matcher.find()) {
			JSONArray classesJson = new JSONArray(matcher.group(1));
			List<String> classes = new ArrayList<String>();
			for(int i = 0; i < classesJson.length(); i++) {
				classes.add(classesJson.getString(i));
			}
			return classes;
		} else {
			throw new IOException();
		}
	}
	
	private VertretungsplanTag getTagByDatum(List<VertretungsplanTag> tage, String datum) {
		for (VertretungsplanTag tag:tage) {
			if (tag.getDatum().equals(datum))
				return tag;
		}
		return new VertretungsplanTag();
	}
	
	private void writeTagByDatum(List<VertretungsplanTag> tage, VertretungsplanTag newTag) {
		int i = 0;
		for (VertretungsplanTag tag:tage) {
			if (tag.getDatum().equals(newTag.getDatum())) {
				tage.set(i, newTag);
				return;
			}
			i++;
		}
		tage.add(newTag);
	}

}
