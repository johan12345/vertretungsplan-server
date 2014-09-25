package com.johan.vertretungsplan.parser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.johan.vertretungsplan.objects.AdditionalInfo;
import com.johan.vertretungsplan.objects.KlassenVertretungsplan;
import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;
import com.paour.comparator.NaturalOrderComparator;

public class DSBMobileParser extends UntisCommonParser {

	private static final String BASE_URL = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB";
	private static final String ENCODING = "UTF-8";
	
	public DSBMobileParser(Schule schule) {
		super(schule);
	}

	@Override
	public Vertretungsplan getVertretungsplan() throws IOException,
			JSONException {
		if (password == null || password.equals(""))
			throw new IOException("no login");
		
		String login = schule.getData().getString("login");
				
		String response = httpGet(BASE_URL + "/authid/" + login + "/" + password, ENCODING);
		if (response.equals("\"00000000-0000-0000-0000-000000000000\""))
			throw new IOException("wrong login");
		
		String authId = response.replace("\"", "");
		
		String response2 = httpGet(BASE_URL + "/timetables/" + authId, ENCODING);
		JSONArray json = new JSONArray(response2);
		
		LinkedHashMap<String, VertretungsplanTag> tage = new LinkedHashMap<String, VertretungsplanTag>();
		for (int i = 0; i<json.length(); i++) {
			String url = json.getJSONObject(i).getString("timetableurl");
			String html = httpGet(url, schule.getData().getString("encoding"));
			Document doc = Jsoup.parse(html);
			if (doc.title().toLowerCase().contains("untis") || doc.html().toLowerCase().contains("untis")) {
				VertretungsplanTag tag = parseMonitorVertretungsplanTag(doc, schule.getData());
				if(!tage.containsKey(tag.getDatum())) {
					tage.put(tag.getDatum(), tag);
				} else {
					VertretungsplanTag tagToMerge = tage.get(tag.getDatum());
					tagToMerge.merge(tag);
					tage.put(tag.getDatum(), tagToMerge);
				}
			} else {
				throw new IOException("Kein Untis-Vertretungsplan?");
			}
		}
		if (schule.getData().optBoolean("sort", false)) {
			for (VertretungsplanTag tag:tage.values()) {
				List<Map.Entry<String, KlassenVertretungsplan>> entries = new ArrayList<Map.Entry<String, KlassenVertretungsplan>>(
						tag.getKlassen().entrySet());
				Collections.sort(entries,
						new Comparator<Map.Entry<String, KlassenVertretungsplan>>() {
							public int compare(
									Map.Entry<String, KlassenVertretungsplan> a,
									Map.Entry<String, KlassenVertretungsplan> b) {
								return new NaturalOrderComparator().compare(a.getKey(),
										b.getKey());
							}
						});
				LinkedHashMap<String, KlassenVertretungsplan> sortedMap = new LinkedHashMap<String, KlassenVertretungsplan>();
				for (Map.Entry<String, KlassenVertretungsplan> entry : entries) {
					sortedMap.put(entry.getKey(), entry.getValue());
				}
				tag.setKlassen(sortedMap);
			}
		}
		Vertretungsplan v = new Vertretungsplan();
		v.setTage(new ArrayList<VertretungsplanTag>(tage.values()));
		
		String response3 = httpGet(BASE_URL + "/news/" + authId, ENCODING);
		JSONArray json2 = new JSONArray(response3);
		
		List<AdditionalInfo> infos = new ArrayList<AdditionalInfo>();
		
		for (int i = 0; i < json2.length(); i++) {
			JSONObject news = json2.getJSONObject(i);
			if (!news.getString("newsid").equals("00000000-0000-0000-0000-000000000000")) {
				AdditionalInfo info = new AdditionalInfo();
				info.setHasInformation(false);
				info.setTitle(news.getString("headline") + " (" + news.getString("newsdate") + ")");
				info.setText(news.getString("wholemessage"));
				infos.add(info);
			}
		}
		
		v.getAdditionalInfos().addAll(infos);
		
		return v;
	}

	@Override
	public List<String> getAllClasses() throws IOException, JSONException {
		JSONArray classesJson = schule.getData().getJSONArray("classes");
		List<String> classes = new ArrayList<String>();
		for(int i = 0; i < classesJson.length(); i++) {
			classes.add(classesJson.getString(i));
		}
		return classes;
	}
	
	/*
	 * Gets the value for every query parameter in the URL. If a parameter name
	 * occurs twice or more, only the first occurance is interpreted by this
	 * method
	 */
	public static Map<String, String> getQueryParamsFirst(String url) {
		try {
			Map<String, String> params = new HashMap<String, String>();
			String[] urlParts = url.split("\\?");
			if (urlParts.length > 1) {
				String query = urlParts[1];
				for (String param : query.split("&")) {
					String[] pair = param.split("=");
					String key = URLDecoder.decode(pair[0], "UTF-8");
					String value = "";
					if (pair.length > 1) {
						value = URLDecoder.decode(pair[1], "UTF-8");
					}

					String values = params.get(key);
					if (values == null) {
						params.put(key, value);
					}
				}
			}

			return params;
		} catch (UnsupportedEncodingException ex) {
			throw new AssertionError(ex);
		}
	}

}
