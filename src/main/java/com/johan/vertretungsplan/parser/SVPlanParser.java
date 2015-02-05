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

import com.johan.vertretungsplan.objects.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Parser für Vertretungspläne der Software svPlan
 * z.B: http://www.ratsschule.de/Vplan/PH_heute.htm
 */
public class SVPlanParser extends BaseParser {

    public SVPlanParser(Schule schule) {
        super(schule);
    }

    public Vertretungsplan getVertretungsplan() throws IOException, JSONException {
        new LoginHandler(schule).handleLogin(executor, cookieStore, username, password); //

        JSONArray urls = schule.getData().getJSONArray("urls");
        String encoding = schule.getData().getString("encoding");
        List<Document> docs = new ArrayList<Document>();

        for (int i = 0; i < urls.length(); i++) {
            JSONObject url = urls.getJSONObject(i);
            loadUrl(url.getString("url"), encoding, docs);
        }

        LinkedHashMap<String, VertretungsplanTag> tage = new LinkedHashMap<String, VertretungsplanTag>();
        for (Document doc : docs) {
            if (doc.select(".svp-tabelle").size() > 0) {
                VertretungsplanTag tag = new VertretungsplanTag();
                String date = doc.select(".svp-plandatum-heute, .svp-plandatum-morgen").text();
                tag.setDatum(date);
                if (doc.select(".svp-uploaddatum").size() > 0)
                    tag.setStand(doc.select(".svp-uploaddatum").text().replace("Aktualisierung: ", ""));

                Elements rows = doc.select(".svp-tabelle tr");
                String lastLesson = "";
                for (Element row : rows) {
                    if (row.hasClass("svp-header"))
                        continue;

                    Vertretung vertretung = new Vertretung();
                    List<String> affectedClasses = new ArrayList<String>();

                    for (Element column : row.select("td")) {
                        if (!hasData(column.text())) {
                            continue;
                        }
                        String type = column.className();
                        if (type.startsWith("svp-stunde")) {
                            vertretung.setLesson(column.text());
                            lastLesson = column.text();
                        } else if (type.startsWith("svp-klasse"))
                            affectedClasses = Arrays.asList(column.text().split(", "));
                        else if (type.startsWith("svp-esfehlt"))
                            vertretung.setPreviousTeacher(column.text());
                        else if (type.startsWith("svp-esvertritt"))
                            vertretung.setTeacher(column.text());
                        else if (type.startsWith("svp-fach"))
                            vertretung.setSubject(column.text());
                        else if (type.startsWith("svp-bemerkung")) {
                            vertretung.setDesc(column.text());
                            vertretung.setType(recognizeType(column.text()));
                        }
                        else if (type.startsWith("svp-raum"))
                            vertretung.setRoom(column.text());

                        if (vertretung.getLesson() == null)
                            vertretung.setLesson(lastLesson);
                    }

                    if (vertretung.getType() == null) {
                        vertretung.setType("Vertretung");
                    }

                    for (String klasse : affectedClasses) {
                        KlassenVertretungsplan kv = tag.getKlassen().get(klasse);
                        if (kv == null)
                            kv = new KlassenVertretungsplan(klasse);
                        kv.add(vertretung);
                        tag.getKlassen().put(klasse, kv);
                    }
                }

                List<String> nachrichten = new ArrayList<String>();
                if (doc.select("h2:contains(Mitteilungen)").size() > 0) {
                    Element h2 = doc.select("h2:contains(Mitteilungen)").first();
                    Element sibling = h2.nextElementSibling();
                    while (sibling != null && sibling.tagName().equals("p")) {
                        for (String nachricht : TextNode.createFromEncoded(sibling.html(), null).getWholeText().split("\n\n")) {
                            if (hasData(nachricht))
                                nachrichten.add(nachricht);
                        }
                        sibling = sibling.nextElementSibling();
                    }
                }
                tag.setNachrichten(nachrichten);

                tage.put(date, tag);
            } else {
                throw new IOException("keine SVPlan-Tabelle gefunden");
            }
        }
        Vertretungsplan v = new Vertretungsplan();
        v.setTage(new ArrayList<VertretungsplanTag>(tage.values()));

        return v;
    }

    private void loadUrl(String url, String encoding, List<Document> docs) throws IOException {
        String html = httpGet(url, encoding).replace("&nbsp;", "");
        Document doc = Jsoup.parse(html);
        docs.add(doc);
    }

    public List<String> getAllClasses() throws JSONException {
        JSONArray classesJson = schule.getData().getJSONArray("classes");
        List<String> classes = new ArrayList<String>();
        for (int i = 0; i < classesJson.length(); i++) {
            classes.add(classesJson.getString(i));
        }
        return classes;
    }

    private boolean hasData(String text) {
        return !text.trim().equals("") && !text.trim().equals("---");
    }
}
