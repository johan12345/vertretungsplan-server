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

import com.johan.vertretungsplan.objects.KlassenVertretungsplan;
import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretung;
import com.johan.vertretungsplan.objects.VertretungsplanTag;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enthält gemeinsam genutzte Funktionen für die Parser für
 * Untis-Vertretungspläne
 *
 */
public abstract class UntisCommonParser extends BaseParser {

	public UntisCommonParser(Schule schule) {
		super(schule);
	}

	private static final String[] EXCLUDED_CLASS_NAMES = new String[] { "-----" };

	/**
	 * Parst eine Vertretungstabelle eines Untis-Vertretungsplans
	 * 
	 * @param table
	 *            das <code>table</code>-Element des HTML-Dokuments, das geparst
	 *            werden soll
	 * @param data
	 *            Daten von der Schule (aus <code>Schule.getData()</code>)
	 * @param tag
	 *            der {@link VertretungsplanTag} in dem die Vertretungen
	 *            gespeichert werden sollen
	 * @throws JSONException
	 */
	protected void parseVertretungsplanTable(Element table, JSONObject data,
			VertretungsplanTag tag) throws JSONException {
		if (data.optBoolean("class_in_extra_line")) {
			for (Element element : table.select("td.inline_header")) {
				String className = getClassName(element.text(), data);
				if (isValidClass(className)) {
					KlassenVertretungsplan kv = new KlassenVertretungsplan(
							className);

					Element zeile = null;
					try {
						zeile = element.parent().nextElementSibling();
						if (zeile.select("td") == null) {
							zeile = zeile.nextElementSibling();
						}
						while (zeile != null
								&& !zeile.select("td").attr("class")
										.equals("list inline_header")) {
							Vertretung v = new Vertretung();

							int i = 0;
							for (Element spalte : zeile.select("td")) {
								if (!hasData(spalte.text())) {
									i++;
									continue;
								}
								String type = data.getJSONArray("columns")
										.getString(i);
								if (type.equals("lesson"))
									v.setLesson(spalte.text());
								else if (type.equals("subject"))
									v.setSubject(spalte.text());
								else if (type.equals("previousSubject"))
									v.setPreviousSubject(spalte.text());
								else if (type.equals("type"))
									v.setType(spalte.text());
								else if (type.equals("type-entfall")) {
									if (spalte.text().equals("x"))
										v.setType("Entfall");
									else
										v.setType("Vertretung");
								} else if (type.equals("room"))
									v.setRoom(spalte.text());
								else if (type.equals("teacher"))
									v.setTeacher(spalte.text());
								else if (type.equals("previousTeacher"))
									v.setPreviousTeacher(spalte.text());
								else if (type.equals("desc"))
									v.setDesc(spalte.text());
								else if (type.equals("desc-type")) {
									v.setDesc(spalte.text());
									v.setType(recognizeType(spalte.text()));
								} else if (type.equals("previousRoom"))
									v.setPreviousRoom(spalte.text());
								i++;
							}

							if (v.getType() == null)
								v.setType("Vertretung");

							if (!v.getLesson().equals("")) {
								kv.add(v);
							}

							zeile = zeile.nextElementSibling();

						}
						tag.getKlassen().put(className, kv);
					} catch (Throwable e) {

						e.printStackTrace();
					}
				}
			}
		} else {
			boolean hasType = false;
			for (int i = 0; i < data.getJSONArray("columns").length(); i++) {
				if (data.getJSONArray("columns").getString(i).equals("type"))
					hasType = true;
			}
			for (Element zeile : table
					.select("tr.list.odd:not(:has(td.inline_header)), "
							+ "tr.list.even:not(:has(td.inline_header)), "
							+ "tr:has(td[align=center]:has(font[color]))")) {
				Vertretung v = new Vertretung();
				String klassen = "";
				int i = 0;
				for (Element spalte : zeile.select("td")) {
					if (!hasData(spalte.text())) {
						i++;
						continue;
					}
					String type = data.getJSONArray("columns").getString(i);
					if (type.equals("lesson"))
						v.setLesson(spalte.text());
					else if (type.equals("subject"))
						v.setSubject(spalte.text());
					else if (type.equals("previousSubject"))
						v.setPreviousSubject(spalte.text());
					else if (type.equals("type"))
						v.setType(spalte.text());
					else if (type.equals("type-entfall")) {
						if (spalte.text().equals("x"))
							v.setType("Entfall");
						else if (!hasType)
							v.setType("Vertretung");
					} else if (type.equals("room"))
						v.setRoom(spalte.text());
					else if (type.equals("previousRoom"))
						v.setPreviousRoom(spalte.text());
					else if (type.equals("desc"))
						v.setDesc(spalte.text());
					else if (type.equals("desc-type")) {
						v.setDesc(spalte.text());
						v.setType(recognizeType(spalte.text()));
					} else if (type.equals("teacher"))
						v.setTeacher(spalte.text());
					else if (type.equals("previousTeacher"))
						v.setPreviousTeacher(spalte.text());
					else if (type.equals("class"))
						klassen = getClassName(spalte.text(), data);
					i++;
				}

				if (v.getType() == null) {
					if (zeile.select("strike").size() > 0 || (v.getSubject() == null && v.getRoom() == null && v.getTeacher() == null && v.getPreviousSubject() != null))
						v.setType("Entfall");
					else
						v.setType("Vertretung");
				}

				List<String> affectedClasses;

				// Detect things like "5-12"
				Pattern pattern = Pattern.compile("(\\d+) ?- ?(\\d+)");
				Matcher matcher = pattern.matcher(klassen);
				if (matcher.find()) {
					affectedClasses = new ArrayList<String>();
					int min = Integer.parseInt(matcher.group(1));
					int max = Integer.parseInt(matcher.group(2));
					try {
						for (String klasse : getAllClasses()) {
							Pattern pattern2 = Pattern.compile("\\d+");
							Matcher matcher2 = pattern2.matcher(klasse);
							if (matcher2.find()) {
								int num = Integer.parseInt(matcher2.group());
								if (min <= num && num <= max)
									affectedClasses.add(klasse);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					if (data.optBoolean("classes_separated", true)) {
						affectedClasses = Arrays.asList(klassen.split(", "));
					} else {
						affectedClasses = new ArrayList<String>();
						try {
							for (String klasse : getAllClasses()) { // TODO:
																	// Gibt es
																	// eine
																	// bessere
																	// Möglichkeit?
								StringBuilder regex = new StringBuilder();
								for (char character : klasse.toCharArray()) {
									regex.append(character);
									regex.append(".*");
								}
								if (klassen.matches(regex.toString()))
									affectedClasses.add(klasse);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				for (String klasse : affectedClasses) {
					if (isValidClass(klasse)) {
						KlassenVertretungsplan kv = tag.getKlassen()
								.get(klasse);
						if (kv == null)
							kv = new KlassenVertretungsplan(klasse);
						kv.add(v);
						tag.getKlassen().put(klasse, kv);
					}
				}
			}
		}
	}

	private boolean hasData(String text) {
		return !text.trim().equals("") && !text.trim().equals("---");
	}

	private String recognizeType(String text) {
		if (text.contains("f.a."))
			return "Entfall";
		else
			return null;
	}

	private String getClassName(String text, JSONObject data) {
		text = text.replace("(", "").replace(")", "");
		if (data.has("classRegex")) {
			Pattern pattern = Pattern.compile(data.getString("classRegex"));
			Matcher matcher = pattern.matcher(text);
			if (matcher.find())
				if (matcher.groupCount() > 0)
					return matcher.group(1);
				else
					return matcher.group();
			else
				return null;
		} else {
			return text;
		}
	}

	/**
	 * Parst eine "Nachrichten zum Tag"-Tabelle aus Untis-Vertretungsplänen
	 * 
	 * @param table
	 *            das <code>table</code>-Element des HTML-Dokuments, das geparst
	 *            werden soll
	 * @param data
	 *            Daten von der Schule (aus <code>Schule.getData()</code>)
	 * @param tag
	 *            der {@link VertretungsplanTag} in dem die Nachrichten
	 *            gespeichert werden sollen
	 */
	protected void parseNachrichten(Element table, JSONObject data,
			VertretungsplanTag tag) {
		Elements zeilen = table
				.select("tr:not(:contains(Nachrichten zum Tag))");
		for (Element i : zeilen) {
			Elements spalten = i.select("td");
			String info = "";
			for (Element b : spalten) {
				info += "\n"
						+ TextNode.createFromEncoded(b.html(), null)
								.getWholeText();
			}
			info = info.substring(1); // remove first \n
			tag.getNachrichten().add(info);
		}
	}

	protected VertretungsplanTag parseMonitorVertretungsplanTag(Document doc,
			JSONObject data) throws JSONException {
		VertretungsplanTag tag = new VertretungsplanTag();
		tag.setDatum(doc.select(".mon_title").first().text()
				.replaceAll(" \\(Seite \\d / \\d\\)", ""));
		if (doc.select("table.mon_head td[align=right] p").size() == 0
				|| schule.getData().optBoolean("stand_links", false)) {
			tag.setStand(doc.select("body").html()
					.substring(0, doc.select("body").html().indexOf("<p>") - 1));
		} else {
			Element stand = doc.select("table.mon_head td[align=right] p")
					.first();
			String info = stand.text();
			tag.setStand(info.substring(info.indexOf("Stand:")));
		}

		// NACHRICHTEN
		if (doc.select("table.info").size() > 0)
			parseNachrichten(doc.select("table.info").first(), data, tag);

		// VERTRETUNGSPLAN
		parseVertretungsplanTable(doc.select("table:has(tr.list)").first(), data, tag);

		return tag;
	}

	private boolean isValidClass(String klasse) {
		if (klasse == null)
			return false;
		if (Arrays.asList(EXCLUDED_CLASS_NAMES).contains(klasse)) {
			return false;
		} else if (schule.getData().has("exclude_classes")
				&& contains(schule.getData().getJSONArray("exclude_classes"),
						klasse)) {
			return false;
		}
		return true;
	}

	private boolean contains(JSONArray array, String string) {
		for (int i = 0; i < array.length(); i++) {
			if (array.getString(i).equals(string))
				return true;
		}
		return false;
	}
}