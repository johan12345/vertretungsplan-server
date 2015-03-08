package com.johan.vertretungsplan.backend;

import com.johan.vertretungsplan.exception.NoCredentialsAvailableException;
import com.johan.vertretungsplan.objects.Schule;
import com.mongodb.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.types.BasicBSONList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;

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
                        if (ex instanceof NoCredentialsAvailableException)
                            continue;
                        // resp.getWriter().println();
						// ex.printStackTrace(resp.getWriter());
						// resp.getWriter().println("---------------------");

						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						ex.printStackTrace(pw);

						log.put("result", "error");
						log.put("stack_trace_2", sw.toString());
                        log.put("stack_trace", arrayToString(ExceptionUtils.getRootCauseStackTrace(ex), "\n"));
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

    private String arrayToString(String[] strings, String separator) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String string:strings) {
            if (first)
                first = false;
            else
                builder.append(separator);
            builder.append(string);
        }
        return builder.toString();
    }

}
