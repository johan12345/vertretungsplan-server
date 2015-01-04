package com.johan.vertretungsplan.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;

public class DSBLightParser extends UntisCommonParser {

	private static final String BASE_URL = "https://light.dsbcontrol.de/DSBlightWebsite/Homepage/";
	private static final String ENCODING = "UTF-8";
	

	public DSBLightParser(Schule schule) {
		super(schule);
	}


	@Override
	public Vertretungsplan getVertretungsplan() throws IOException,
			JSONException {
		String id = schule.getData().getString("id");
		LinkedHashMap<String, VertretungsplanTag> tage = new LinkedHashMap<String, VertretungsplanTag>();

		Map<String, String> referer = new HashMap<String, String>();
		referer.put("Referer", BASE_URL + "/Player.aspx?ID=" + id);

		String response = httpGet(BASE_URL + "/IFrame.aspx?ID=" + id, ENCODING,
				referer);
		Document doc = Jsoup.parse(response);
		Pattern regex = Pattern.compile("location\\.href=\"([^\"]*)\"");
		for (Element iframe : doc.select("iframe")) {
			String response2 = httpGet(iframe.attr("src"), ENCODING, referer);
			Matcher matcher = regex.matcher(response2);
			if (matcher.find()) {
				String url = matcher.group(1);
				String response3 = httpGet(url, ENCODING, referer);
				Document doc2 = Jsoup.parse(response3);
				for (Element iframe2 : doc2.select("iframe")) {
					String response4 = httpGet(iframe2.attr("src"), schule
							.getData().getString("encoding"), referer);
					Document doc3 = Jsoup.parse(response4);
					if (doc3.title().toLowerCase().contains("untis")
							|| doc3.html().toLowerCase().contains("untis")) {
						VertretungsplanTag tag = parseMonitorVertretungsplanTag(
								doc3, schule.getData());
						if (!tage.containsKey(tag.getDatum())) {
							tage.put(tag.getDatum(), tag);
						} else {
							VertretungsplanTag tagToMerge = tage.get(tag
									.getDatum());
							tagToMerge.merge(tag);
							tage.put(tag.getDatum(), tagToMerge);
						}
					} else {
						throw new IOException("Kein Untis-Vertretungsplan?");
					}
				}
			} else {
				throw new IOException("URL nicht gefunden");
			}
		}

		Vertretungsplan v = new Vertretungsplan();
		v.setTage(new ArrayList<VertretungsplanTag>(tage.values()));

		return v;
	}

	@Override
	public List<String> getAllClasses() throws IOException, JSONException {
		JSONArray classesJson = schule.getData().getJSONArray("classes");
		List<String> classes = new ArrayList<String>();
		for (int i = 0; i < classesJson.length(); i++) {
			classes.add(classesJson.getString(i));
		}
		return classes;
	}

}
