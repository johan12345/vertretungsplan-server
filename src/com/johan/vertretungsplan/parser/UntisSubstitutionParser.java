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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.johan.vertretungsplan.objects.KlassenVertretungsplan;
import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;

/**
 * Parser für Untis-Vertretungspläne mit dem Vertretungsplanungs-Layout
 * Beispiel: http://www.jkg-stuttgart.de/vertretungsplan/sa3.htm
 *
 */
public class UntisSubstitutionParser extends UntisCommonParser {

	private String baseUrl;
	private JSONObject data;

	public UntisSubstitutionParser(Schule schule) {
		super(schule);
		try {
			data = schule.getData();
			baseUrl = data.getString("baseurl");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Vertretungsplan getVertretungsplan() throws IOException,
			JSONException {
		new LoginHandler(schule).handleLogin(executor, cookieStore, username,
				password);

		String encoding = schule.getData().getString("encoding");
		Document doc = Jsoup.parse(this.httpGet(baseUrl, encoding));
		Elements classes = doc.select("td a");

		Vertretungsplan v = new Vertretungsplan();
		List<VertretungsplanTag> tage = new ArrayList<VertretungsplanTag>();
		VertretungsplanTag tag = new VertretungsplanTag();
		tage.add(tag);
		v.setTage(tage);

		String stand = doc.select("td[align=right]:not(:has(b))").text();
		tag.setStand(stand);

		Pattern dayPattern = Pattern.compile("\\d\\d?.\\d\\d?. / \\w+");

		for (Element klasse : classes) {
			Document classDoc = Jsoup.parse(httpGet(
					baseUrl.substring(0, baseUrl.lastIndexOf("/"))
							+ "/" + klasse.attr("href"), encoding));
			if (tag.getDatum() == null) {
				String title = classDoc.select("font[size=5]").text();
				Matcher matcher = dayPattern.matcher(title);
				if (matcher.find())
					tag.setDatum(matcher.group());
			}

			Element table = classDoc.select("table[rules=all]").first();
			parseVertretungsplanTable(table, data, tag);
		}
		return v;
	}

	@Override
	public List<String> getAllClasses() throws JSONException, IOException {
		JSONArray classesJson = schule.getData().getJSONArray("classes");
		List<String> classes = new ArrayList<String>();
		for (int i = 0; i < classesJson.length(); i++) {
			classes.add(classesJson.getString(i));
		}
		return classes;
	}

}
