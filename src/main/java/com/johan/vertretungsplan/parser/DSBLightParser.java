package com.johan.vertretungsplan.parser;

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        String response = httpGet(BASE_URL + "/Player.aspx?ID=" + id, ENCODING,
                referer);
        Document doc = Jsoup.parse(response);
        // IFrame.aspx
        String iframeUrl = doc.select("iframe").first().attr("src");

        response = httpGet(iframeUrl, ENCODING, referer);

        doc = Jsoup.parse(response);

        if (schule.getData().has("login") && schule.getData().getBoolean("login")) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("__VIEWSTATE", doc.select(
                    "#__VIEWSTATE").attr("value")));
            params.add(new BasicNameValuePair("__VIEWSTATEGENERATOR", doc.select(
                    "#__VIEWSTATEGENERATOR").attr("value")));
            params.add(new BasicNameValuePair("__EVENTVALIDATION", doc.select(
                    "#__EVENTVALIDATION").attr("value")));
            params.add(new BasicNameValuePair("ctl02$txtBenutzername", getUsername()));
            params.add(new BasicNameValuePair("ctl02$txtPasswort", getPassword()));
            params.add(new BasicNameValuePair("ctl02$btnLogin", "weiter"));
            response = httpPost(iframeUrl, ENCODING, params, referer);
            doc = Jsoup.parse(response);
        }
        Pattern regex = Pattern.compile("location\\.href=\"([^\"]*)\"");

        for (Element iframe : doc.select("iframe")) {
            // PreProgram.aspx
            String response2 = httpGet(iframe.attr("src"), ENCODING, referer);
            Matcher matcher = regex.matcher(response2);
            if (matcher.find()) {
                // Program.aspx
                String url = matcher.group(1);
                parseProgram(url, tage, referer);
            } else {
                throw new IOException("URL nicht gefunden");
            }
        }

        Vertretungsplan v = new Vertretungsplan();
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
        v.setTage(tageList);

        return v;
    }

    private void parseProgram(String url, LinkedHashMap<String, VertretungsplanTag> tage, Map<String, String> referer) throws IOException {
        parseProgram(url, tage, referer, null);
    }

    private void parseProgram(String url, LinkedHashMap<String, VertretungsplanTag> tage, Map<String, String> referer, String firstUrl) throws IOException {
        String response = httpGet(url, ENCODING, referer);
        Document doc = Jsoup.parse(response, url);
        if (doc.select("iframe").attr("src").equals(firstUrl))
            return;
        for (Element iframe : doc.select("iframe")) {
            // Data
            parseTag(iframe.attr("src"), referer, tage, iframe.attr("src"));
        }
        if (doc.select("#hlNext").size() > 0) {
            String nextUrl = doc.select("#hlNext").first().attr("abs:href");
            if (firstUrl == null)
                parseProgram(nextUrl, tage, referer, doc.select("iframe").attr("src"));
            else
                parseProgram(nextUrl, tage, referer, firstUrl);
        }
    }

    private void parseTag(String url, Map<String, String> referer, LinkedHashMap<String, VertretungsplanTag> tage, String startUrl) throws IOException {
        String html = httpGet(url, schule.getData().getString("encoding"), referer);
        Document doc = Jsoup.parse(html);
        if (doc.title().toLowerCase().contains("untis")
                || doc.html().toLowerCase().contains("untis")) {
            VertretungsplanTag tag = parseMonitorVertretungsplanTag(
                    doc, schule.getData());
            if (!tage.containsKey(tag.getDatum())) {
                tage.put(tag.getDatum(), tag);
            } else {
                VertretungsplanTag tagToMerge = tage.get(tag
                        .getDatum());
                tagToMerge.merge(tag);
                tage.put(tag.getDatum(), tagToMerge);
            }
            if (doc.select("meta[http-equiv=refresh]").size() > 0) {
                Element meta = doc.select("meta[http-equiv=refresh]").first();
                String attr = meta.attr("content").toLowerCase();
                String redirectUrl = url.substring(0, url.lastIndexOf("/") + 1) +
                        attr.substring(attr.indexOf("url=") + 4);
                if (!redirectUrl.equals(startUrl))
                    parseTag(redirectUrl, referer, tage, startUrl);
            }
        }
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
