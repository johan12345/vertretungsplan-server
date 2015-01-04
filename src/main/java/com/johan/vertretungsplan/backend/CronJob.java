package com.johan.vertretungsplan.backend;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bson.types.BasicBSONList;

import com.johan.vertretungsplan.objects.Schule;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class CronJob implements Runnable {

	@Override
	public void run() {
		try {
			MongoClient client = DBManager.getInstance();
			DB db = client.getDB("vertretungsplan");

			DBCollection logsColl = db.getCollection("logs");

			BasicDBObject index = new BasicDBObject("date", 1);
			BasicDBObject options = new BasicDBObject("expireAfterSeconds",
					TimeUnit.DAYS.toSeconds(3));
			logsColl.createIndex(index, options);

			long startTime = Utils.getTimeInMillis();
			DBObject logObj = new BasicDBObject("_id", startTime);
			logObj.put("date", new Date());
			BasicBSONList logMessage = new BasicBSONList();

			final ExecutorService service = Executors.newCachedThreadPool();
			List<Entry<String, Future<ParseThreadResult>>> tasks = new ArrayList<Entry<String, Future<ParseThreadResult>>>();

			for (Schule school : GetSchoolsServlet.getSchools()) {
				tasks.add(new AbstractMap.SimpleEntry<String, Future<ParseThreadResult>>(
						school.getId(),
						service.submit(new ParseThread(school.getId(), school
								.toJSON().toString(), db, false))));
			}
			boolean hasErrors = false;
			for (Entry<String, Future<ParseThreadResult>> entry : tasks) {
				DBObject log = new BasicDBObject("id", entry.getKey());
				try {
					ParseThreadResult result = entry.getValue().get();
					Exception ex = result.ex;
					if (ex != null) {
						// resp.getWriter().println();
						// ex.printStackTrace(resp.getWriter());
						// resp.getWriter().println("---------------------");

						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						ex.printStackTrace(pw);

						log.put("result", "error");
						log.put("stack_trace", sw.toString());
						hasErrors = true;
					} else {
						// resp.getWriter().println("Erfolgreich: " +
						// entry.getKey());

						log.put("result", "success");
						log.put("gcm_result", result.gcmResult);
					}
				} catch (InterruptedException e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					log.put("result", "error");
					log.put("stack_trace", sw.toString());
					hasErrors = true;
				} catch (ExecutionException e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					log.put("result", "error");
					log.put("stack_trace", sw.toString());
					hasErrors = true;
				}
				logMessage.add(log);
			}
			long timeElapsed = Utils.getTimeInMillis() - startTime;
			logObj.put("time_elapsed", ((double) timeElapsed) / 1000);
			logObj.put("results", logMessage);
			logObj.put("hasErrors", hasErrors);
			logsColl.save(logObj);

			service.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
