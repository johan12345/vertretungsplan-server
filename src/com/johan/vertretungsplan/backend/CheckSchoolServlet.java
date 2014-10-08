package com.johan.vertretungsplan.backend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.johan.vertretungsplan.backend.ParseThread.ParseResult;

@SuppressWarnings("serial")
public class CheckSchoolServlet extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws FileNotFoundException, IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = req.getReader();
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} finally {
			reader.close();
		}
		String json = sb.toString();

		try {
			ParseResult result = new ParseThread(null, json, DBManager
					.getInstance().getDB("vertretungsplan"), true).parse();
			String res = new Gson().toJson(result);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().print(res);
			resp.getWriter().close();
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setCharacterEncoding("UTF-8");
			e.printStackTrace(resp.getWriter());
			resp.getWriter().close();
		}

	}
}
