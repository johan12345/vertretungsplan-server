package com.johan.vertretungsplan.parser;

import com.johan.vertretungsplan.objects.*;
import org.apache.http.client.fluent.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Generischer Parser für Vertretungspläne im CSV-Format
 * Beispiel: http://czg.noxxi.de/vp.pl?/csv
 */
public class CSVParser extends BaseParser {

    private JSONObject data;

    public CSVParser(Schule schule) {
        super(schule);
        data = schule.getData();
    }

    @Override
    public Vertretungsplan getVertretungsplan() throws IOException, JSONException {
        new LoginHandler(schule).handleLogin(executor, cookieStore, username, password);
        String url = data.getString("url");
        String response = executor.execute(Request.Get(url)).returnContent().asString();

        Vertretungsplan vertretungsplan = new Vertretungsplan();
        LinkedHashMap<String, VertretungsplanTag> days = new LinkedHashMap<>();

        String[] lines = response.split("\n");

        String separator = data.getString("separator");
        for (int i = data.optInt("skipLines", 0); i<lines.length; i++) {
            String[] columns = lines[i].split(separator);
            Vertretung v = new Vertretung();
            String klasse = null;
            String day = null;
            String stand = "";
            for (String column:columns) {
                String type = data.getJSONArray("columns")
                        .getString(i);
                if (type.equals("lesson"))
                    v.setLesson(column);
                else if (type.equals("subject"))
                    v.setSubject(column);
                else if (type.equals("previousSubject"))
                    v.setPreviousSubject(column);
                else if (type.equals("type"))
                    v.setType(column);
                else if (type.equals("type-entfall")) {
                    if (column.equals("x"))
                        v.setType("Entfall");
                    else
                        v.setType("Vertretung");
                } else if (type.equals("room"))
                    v.setRoom(column);
                else if (type.equals("teacher"))
                    v.setTeacher(column);
                else if (type.equals("previousTeacher"))
                    v.setPreviousTeacher(column);
                else if (type.equals("desc"))
                    v.setDesc(column);
                else if (type.equals("desc-type")) {
                    v.setDesc(column);
                    v.setType(recognizeType(column));
                } else if (type.equals("previousRoom"))
                    v.setPreviousRoom(column);
                else if (type.equals("class"))
                    klasse = getClassName(column, data);
                else if (type.equals("day"))
                    day = column;
                else if (type.equals("stand"))
                    stand = column;
            }
            if (v.getType() == null)
                v.setType("Vertretung");

            if (isValidClass(klasse) && day != null) {
                VertretungsplanTag tag = days.get(day);
                if (tag == null) {
                    tag = new VertretungsplanTag();
                    tag.setDatum(day);
                    tag.setStand(stand);
                }
                KlassenVertretungsplan kv = tag.getKlassen()
                        .get(klasse);
                if (kv == null)
                    kv = new KlassenVertretungsplan(klasse);
                kv.add(v);
                tag.getKlassen().put(klasse, kv);
            }
        }
        vertretungsplan.setTage(new ArrayList<>(days.values()));
        return vertretungsplan;
    }

    @Override
    public List<String> getAllClasses() throws IOException, JSONException {
        JSONArray classesJson = schule.getData().getJSONArray("classes");
        List<String> classes = new ArrayList<>();
        for(int i = 0; i < classesJson.length(); i++) {
            classes.add(classesJson.getString(i));
        }
        return classes;
    }

    private boolean isValidClass(String klasse) {
        if (klasse == null)
            return false;
        else if (data.has("exclude_classes")
                && contains(schule.getData().getJSONArray("exclude_classes"),
                klasse)) {
            return false;
        }
        return true;
    }
}
