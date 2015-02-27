package com.johan.vertretungsplan.parser;

import com.johan.vertretungsplan.objects.*;
import com.paour.comparator.NaturalOrderComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DSBMobileParser extends UntisCommonParser {

	public DSBMobileParser(Schule schule) {
		super(schule);
	}

	private static final String BASE_URL = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB";
	private static final String ENCODING = "UTF-8";

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
				if (doc.select(".mon_head").size() > 1) {
					for (int j = 0; j < doc.select(".mon_head").size(); j++) {
						Document doc2 = Document.createShell(doc.baseUri());
						doc2.body().appendChild(doc.select(".mon_head").get(j).clone());
                        Element next = doc.select(".mon_head").get(j).nextElementSibling();
                        if (next != null && next.tagName().equals("center")) {
                            doc2.body().appendChild(next.select(".mon_title").first().clone());
                            if (next.select("table:has(tr.list)").size() > 0)
                                doc2.body().appendChild(next.select("table:has(tr.list)").first());
                            if (next.select("table.info").size() > 0)
                                doc2.body().appendChild(next.select("table.info").first());
                        } else {
                            doc2.body().appendChild(doc.select(".mon_title").get(j).clone());
                            doc2.body().appendChild(doc.select("table:has(tr.list)").get(j).clone());
                        }
						VertretungsplanTag tag = parseMonitorVertretungsplanTag(doc2, schule.getData());
						if (!tage.containsKey(tag.getDatum())) {
							tage.put(tag.getDatum(), tag);
						} else {
							VertretungsplanTag tagToMerge = tage.get(tag.getDatum());
							tagToMerge.merge(tag);
							tage.put(tag.getDatum(), tagToMerge);
						}
					}
				} else {
					VertretungsplanTag tag = parseMonitorVertretungsplanTag(doc, schule.getData());
					if (!tage.containsKey(tag.getDatum())) {
						tage.put(tag.getDatum(), tag);
					} else {
						VertretungsplanTag tagToMerge = tage.get(tag.getDatum());
						tagToMerge.merge(tag);
						tage.put(tag.getDatum(), tagToMerge);
					}
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
		List<VertretungsplanTag> tageList = new ArrayList<VertretungsplanTag>(tage.values());
		Collections.sort(tageList, new Comparator<VertretungsplanTag>() {

			@Override
			public int compare(VertretungsplanTag o1, VertretungsplanTag o2) {
                // Check if dates are parseable, else compare strings
                Pattern pattern = Pattern.compile("(\\d\\d?)\\.(\\d\\d?)\\.(\\d\\d\\d?\\d?)");
                Matcher matcher1 = pattern.matcher(o1.getDatum());
                Matcher matcher2 = pattern.matcher(o2.getDatum());
                if (matcher1.find() && matcher2.find()) {
                    if (!matcher1.group(3).equals(matcher2.group(3)))
                        return matcher1.group(3).compareTo(matcher2.group(3));
                    else if (!matcher1.group(2).equals(matcher2.group(2)))
                        return matcher1.group(2).compareTo(matcher2.group(2));
                    else
                        return matcher1.group(1).compareTo(matcher2.group(1));
                } else {
                    return o1.getDatum().compareTo(o2.getDatum());
                }
			}
			
		});
		Vertretungsplan v = new Vertretungsplan();
		v.setTage(tageList);
		
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
