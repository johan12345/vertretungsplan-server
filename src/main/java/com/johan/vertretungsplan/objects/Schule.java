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

package com.johan.vertretungsplan.objects;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Schule {
	private String id;
	private String name;
	private String city;
	private String api;
	private List<String> additionalInfos;
	private JSONObject data;
	private double[] geo;
	
	
	public static Schule fromJSON(String id, JSONObject json) throws JSONException {
		Schule schule = new Schule();
		schule.setId(id);
		schule.setCity(json.getString("city"));
		schule.setName(json.getString("name"));
		schule.setApi(json.getString("api"));
		if(json.has("geo")) {
			JSONArray geoArray = json.getJSONArray("geo");
			double[] geo = new double[]{geoArray.getDouble(0), geoArray.getDouble(1)};
			schule.setGeo(geo);
		}
		
		JSONArray infosJson = json.optJSONArray("additional_info");
		List<String> additionalInfos = new ArrayList<String>();
		if(infosJson != null) {
			for (int i = 0; i < infosJson.length(); i++) {
				additionalInfos.add(infosJson.getString(i));
			}
		}
		schule.setAdditionalInfos(additionalInfos);
		
		schule.setData(json.getJSONObject("data"));
		return schule;
	}
	
	public JSONObject toJSON() throws JSONException {
		JSONObject schule = new JSONObject();
		schule.put("city", city);
		schule.put("name", name);
		schule.put("api", api);
		if(geo != null) {
			JSONArray geoArray = new JSONArray();
			geoArray.put(geo[0]);
			geoArray.put(geo[1]);
			schule.put("geo", geoArray);
		}
		
		if(additionalInfos.size() > 0) {
			JSONArray infosJson = new JSONArray();
			for (String info:additionalInfos) {
				infosJson.put(info);
			}
			schule.put("additional_info", infosJson);
		}
		
		schule.put("data", data);
		return schule;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return the api
	 */
	public String getApi() {
		return api;
	}
	/**
	 * @param api the api to set
	 */
	public void setApi(String api) {
		this.api = api;
	}
	/**
	 * @return the additionalInfos
	 */
	public List<String> getAdditionalInfos() {
		return additionalInfos;
	}
	/**
	 * @param additionalInfos the additionalInfos to set
	 */
	public void setAdditionalInfos(List<String> additionalInfos) {
		this.additionalInfos = additionalInfos;
	}
	/**
	 * @return the data
	 */
	public JSONObject getData() {
		return data;
	}
	/**
	 * @param data the data to set
	 */
	public void setData(JSONObject data) {
		this.data = data;
	}
	public double[] getGeo() {
		return geo;
	}
	public void setGeo(double[] geo) {
		this.geo = geo;
	}
}
