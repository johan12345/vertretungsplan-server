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
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;

/**
 * Parser für Untis-Vertretungspläne mit dem Info-Stundenplan-Layout, aber ohne Navigationsleiste
 * Beispiel: http://www.vertretung.org/vertretung/w00000.htm
 * Wurde bisher noch nicht mit anderen Schulen getestet.
 *
 */
public class UntisInfoHeadlessParser extends UntisCommonParser {
	
	private String url;
	private JSONObject data;

	public UntisInfoHeadlessParser(Schule schule) {
		super(schule);
		try {
			data = schule.getData();
			url = data.getString("url");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Vertretungsplan getVertretungsplan()
			throws IOException, JSONException {
		new LoginHandler(schule).handleLogin(executor, cookieStore, username, password);
		
		Vertretungsplan v = new Vertretungsplan();
		List<VertretungsplanTag> tage = new ArrayList<VertretungsplanTag>();
			
		Document doc = Jsoup.parse(httpGet(url, schule.getData().getString("encoding")));
		Elements days = doc.select("#vertretung > p > b, #vertretung > b");
		for(Element day:days) {
			VertretungsplanTag tag = new VertretungsplanTag();
			tag.setStand("");
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
		v.setTage(tage);
		return v;
	}

	@Override
	public List<String> getAllClasses() throws JSONException, IOException {
		JSONArray classesJson = schule.getData().getJSONArray("classes");
		List<String> classes = new ArrayList<String>();
		for(int i = 0; i < classesJson.length(); i++) {
			classes.add(classesJson.getString(i));
		}
		return classes;
	}

}
