package com.johan.vertretungsplan.backend;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class DBManager {
	private static volatile MongoClient mongo = null;
	private DBManager() {}
	public static MongoClient getInstance() {
		if (mongo == null) {
			synchronized (DBManager.class) {
				if (mongo == null) {
					try {
						ServerAddress addr = new ServerAddress(System.getenv("OPENSHIFT_MONGODB_DB_HOST")
								, Integer.valueOf(System.getenv("OPENSHIFT_MONGODB_DB_PORT")));
						MongoCredential cred = MongoCredential.createMongoCRCredential(
								System.getenv("OPENSHIFT_MONGODB_DB_USERNAME"),
								"vertretungsplan",
								System.getenv("OPENSHIFT_MONGODB_DB_PASSWORD").toCharArray());
						List<MongoCredential> creds = new ArrayList<MongoCredential>();
						creds.add(cred);
						mongo = new MongoClient(addr, creds);
					} catch (NumberFormatException e) { 
					} catch (UnknownHostException e) {}
				}
			}
		}
		return mongo;
	}
}
