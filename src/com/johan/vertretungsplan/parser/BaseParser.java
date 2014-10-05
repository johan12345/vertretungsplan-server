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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.json.JSONException;

import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;

/**
 * Ein Parser für einen Vertretungsplan. Er erhält Informationen aus der
 * JSON-Datei für eine Schule und liefert den abgerufenen und geparsten
 * Vertretungsplan zurück.
 */
public abstract class BaseParser {
	/**
	 * Die Schule, deren Vertretungsplan geparst werden soll
	 */
	protected Schule schule;
	protected Executor executor;
	protected String username;
	protected String password;
	protected CookieStore cookieStore;

	public BaseParser(Schule schule) {
		this.schule = schule;
		this.cookieStore = new BasicCookieStore();
		this.executor = Executor.newInstance().cookieStore(cookieStore);
	}

	/**
	 * Ruft den Vertretungsplan ab und parst ihn. Wird immer asynchron
	 * ausgeführt.
	 * 
	 * @return Der geparste {@link Vertretungsplan}
	 * @throws IOException
	 * @throws JSONException
	 */
	public abstract Vertretungsplan getVertretungsplan() throws IOException,
			JSONException;

	/**
	 * Gibt eine Liste aller verfügbaren Klassen zurück. Wird immer asynchron
	 * ausgeführt.
	 * 
	 * @return Eine Liste aller verfügbaren Klassen für diese Schule (auch die,
	 *         die nicht aktuell vom Vertretungsplan betroffen sind)
	 * @throws IOException
	 * @throws JSONException
	 */
	public abstract List<String> getAllClasses() throws IOException,
			JSONException;

	protected String httpGet(String url, String encoding) throws IOException {
		return httpGet(url, encoding, null);
	}

	protected String httpGet(String url, String encoding,
			Map<String, String> headers) throws IOException {
		Request request = Request.Get(url).connectTimeout(15000)
				.socketTimeout(15000);
		if (headers != null) {
			for (Entry<String, String> entry : headers.entrySet()) {
				request.addHeader(entry.getKey(), entry.getValue());
			}
		}
		return new String(executor.execute(request).returnContent().asBytes(),
				encoding);
	}

	protected String httpPost(String url, String encoding,
			List<NameValuePair> formParams) throws IOException {
		return new String(executor
				.execute(
						Request.Post(url).bodyForm(formParams)
								.connectTimeout(15000).socketTimeout(15000))
				.returnContent().asBytes(), encoding);
	}

	/**
	 * Erstelle einen neuen Parser für eine Schule. Liefert automatisch eine
	 * passende Unterklasse.
	 * 
	 * @param schule
	 *            die Schule, für die ein Parser erstellt werden soll
	 * @return Eine Unterklasse von {@link BaseParser}, die zur übergebenen
	 *         Schule passt
	 */
	public static BaseParser getInstance(Schule schule) {
		BaseParser parser = null;
		if (schule != null) {
			if (schule.getApi().equals("untis-monitor")) {
				parser = new UntisMonitorParser(schule);
			} else if (schule.getApi().equals("untis-info")) {
				parser = new UntisInfoParser(schule);
			} else if (schule.getApi().equals("untis-info-headless")) {
				parser = new UntisInfoHeadlessParser(schule);
			} else if (schule.getApi().equals("dsbmobile")) {
				parser = new DSBMobileParser(schule);
			} else if (schule.getApi().equals("dsblight")) {
				parser = new DSBLightParser(schule);
			}

			// else if ... (andere Parser)
		}
		return parser;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
}
