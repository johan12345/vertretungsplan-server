package com.johan.vertretungsplan.backend;


import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.gson.Gson;
import com.johan.vertretungsplan.additionalinfo.BaseAdditionalInfoParser;
import com.johan.vertretungsplan.exception.NoCredentialsAvailableException;
import com.johan.vertretungsplan.objects.AdditionalInfo;
import com.johan.vertretungsplan.objects.Schule;
import com.johan.vertretungsplan.objects.Vertretungsplan;
import com.johan.vertretungsplan.objects.VertretungsplanTag;
import com.johan.vertretungsplan.parser.BaseParser;
import com.mongodb.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParseThread implements Callable<ParseThreadResult> {
	
	private String schoolData;
	private String schoolId;
	private DB db;
	private boolean test;
	public ParseThread(String schoolId, String schoolData, DB db, boolean test) {
		this.schoolId = schoolId;
		this.schoolData = schoolData;
		this.db = db;
		this.test = test;
	}
	public ParseThreadResult call() {
		try {
			Gson gson = new Gson();
			
			ParseResult result = parse();
			if(!test) {
				DBCollection coll = db.getCollection("schools");
				BasicDBObject query = new BasicDBObject("_id", schoolId);
				DBObject school = coll.findOne(query);
				
				Vertretungsplan vAlt = null;
				if(school != null) {
					vAlt = gson.fromJson((String) school.get("vertretungsplan"), Vertretungsplan.class);
				} else {
					school = new BasicDBObject("_id", schoolId);
				}
				
				school.put("vertretungsplan", gson.toJson(result.v));
				school.put("classes", gson.toJson(result.classes));
				coll.save(school);
				
				String gcmResult = "";
				if(vAlt != null) {
					gcmResult = sendMessage(schoolId, 
						changedClasses(vAlt, result.v, result.classes));
				}	
				return new ParseThreadResult(gcmResult);
			} else {
				return new ParseThreadResult("test");
			}
        } catch (JSONException | NumberFormatException | IOException | NoCredentialsAvailableException e) {
            return new ParseThreadResult(e);
        }
    }

    public ParseResult parse() throws IOException, NoCredentialsAvailableException {
        JSONObject json = new JSONObject(schoolData);
		Schule schule = Schule.fromJSON(schoolId, json);
		DBCollection cookiesColl = db.getCollection("cookies");
		
		String login = null;
		String password = null;
		DBObject reg = null;
		DBCollection regColl = null;
		if (schule.getData().has("login") && !test) {
			regColl = db.getCollection("registrations");
			BasicDBObject query2 = new BasicDBObject("schoolId", schoolId);
			query2.append("login", new BasicDBObject("$ne", ""));
			query2.append("password", new BasicDBObject("$ne", ""));
            query2.append("password_invalid", new BasicDBObject("$ne", "true"));
            DBCursor regs = regColl.find(query2);
			int n = regs.count();
            if (n == 0) {
                throw new NoCredentialsAvailableException();
            }
            int rand = (int) Math.floor(Math.random() * (n-1));
			regs.skip(rand);
			reg = regs.next();
			regs.close();
			
			login = (String) reg.get("login");
			password = (String) reg.get("password");
		}
		
		BaseParser parser = BaseParser.getInstance(schule);
		if (login != null && password != null) {
			parser.setUsername(login);
			parser.setPassword(password);
		}
		try {
			Vertretungsplan v = parser.getVertretungsplan();
			v.setSchoolName(schule.getName());
			v.setCity(schule.getCity());
		
			List<BaseAdditionalInfoParser> additionalInfoParsers = new ArrayList<BaseAdditionalInfoParser>();
			for(String type:schule.getAdditionalInfos()) {
				additionalInfoParsers.add(BaseAdditionalInfoParser.getInstance(type));
			}
			
			for(BaseAdditionalInfoParser additionalInfoParser:additionalInfoParsers) {
				v.getAdditionalInfos().add(additionalInfoParser.getAdditionalInfo());
			}
			ParseResult result = new ParseResult();
			result.v = v;
			result.classes = parser.getAllClasses();
			
			if (reg != null) {
				regColl.remove(reg);
				reg.put("password_invalid", false);
				regColl.insert(reg);
			}
			
			return result;
		} catch (IOException e) {
            if (e.getMessage() != null) {
                String msg = e.getMessage().toLowerCase();
                if (reg != null && (msg.contains("login") || msg.contains("authorization"))) {
                    regColl.remove(reg);
                    reg.put("password_invalid", true);
                    regColl.insert(reg);
                }
            }
			cookiesColl.remove(new BasicDBObject("_id", schule.getId()));
			throw e;
		}
	}
	
	public class ParseResult {
		public Vertretungsplan v;
		public List<String> classes;
	}

    public static HashMap<String, ChangeType> changedClasses(Vertretungsplan vAlt, Vertretungsplan
            v, List<String> klassen) {
        HashMap<String, ChangeType> changedClasses = new HashMap<String, ChangeType>();
		for(String klasse:klassen) {
			ChangeType change = somethingChanged(vAlt, v, klasse);
			if(change != null)
				changedClasses.put(klasse, change);
		}
		return changedClasses;
	}

    public static ChangeType somethingChanged(Vertretungsplan vAlt, Vertretungsplan v,
                                              String klasse) {
		
		for(AdditionalInfo info:v.getAdditionalInfos()) {
			if(info.hasInformation()) {
				//passende alte Info finden
				AdditionalInfo oldInfo = null;
				for(AdditionalInfo infoAlt:vAlt.getAdditionalInfos()) {
					if(infoAlt.getText().equals(info.getText())) {
						oldInfo = infoAlt;
						break;
					}
				}
				if(oldInfo == null) {
					//es wurde keine passende alte Info gefunden
					return ChangeType.NOTIFICATION;
				}
			}
		}
		
		for(VertretungsplanTag tag:v.getTage()) {
			//passenden alten Tag finden
			VertretungsplanTag oldTag = null;
			for(VertretungsplanTag tagAlt:vAlt.getTage()) {
				if(tagAlt.getDatum().equals(tag.getDatum())) {
					oldTag = tagAlt;
					break;
				}
			}
			
			if(tag.getKlassen().get(klasse) != null
					&& tag.getKlassen().get(klasse).getVertretung().size() > 0) {
				//Auf dem neuen Plan gibt es Vertretungen, die die gewählte Klasse betreffen
				if(oldTag == null) {
					//dieser Tag wurde neu hinzugefügt -> Vertretungen waren vorher nicht bekannt
					return ChangeType.NOTIFICATION;
				} else {
					//dieser Tag war vorher schon auf dem Vertretungsplan
					//Stand prüfen
					if((oldTag.getStand() == null && tag.getStand() == null) ||
                            !oldTag.getStand().equals(tag.getStand())) {
						//Stand hat sich verändert oder ist nicht verfügbar
						if(oldTag.getKlassen().get(klasse) != null
								&& oldTag.getKlassen().get(klasse).getVertretung().size() > 0) {
							//auch vorher waren schon Vertretungen für die Klasse bekannt
							//-> vergleiche alte mit neuen Vertretungen
							if(!oldTag.getKlassen().get(klasse).getVertretung().equals(
									tag.getKlassen().get(klasse).getVertretung())) {
								//Die Vertretungen sind nicht gleich
                                Logger log = Logger.getGlobal();
                                log.log(Level.INFO, "Änderung " +
                                        "für Schule " + v.getSchoolName());
                                log.log(Level.INFO, "Alter Vertretungsplan:");
                                log.log(Level.INFO, new Gson().toJson(vAlt));
                                log.log(Level.INFO, "Neuer Vertretungsplan:");
                                log.log(Level.INFO, new Gson().toJson(v));
                                return ChangeType.NOTIFICATION;
							} else {
								//keine Veränderung
							}
						} else {
							//vorher waren keine Vertretungen für die gewählte Klasse bekannt -> es wurde etwas verändert
							return ChangeType.NOTIFICATION;
						}
					}
				}
			} else {
				if (oldTag == null) {
					return ChangeType.NO_NOTIFICATION;
				}
			}
		}
		return null;
	}
	
	private String sendMessage(String schoolId,
			HashMap<String, ChangeType> changedClasses) throws NumberFormatException, IOException {
		StringBuilder message = new StringBuilder();
		
		MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");
		
		DBCollection coll = db.getCollection("registrations");
		
		Sender sender = new Sender(Settings.GCM_API_KEY);
		
		for(Entry<String, ChangeType> entry:changedClasses.entrySet()) {
			BasicDBObject query = new BasicDBObject("schoolId", schoolId);
			query.append("klasse", entry.getKey());
			DBCursor cursor = coll.find(query);
			
			int i = 0;
			try {
			   while(cursor.hasNext()) {
			       DBObject sub = cursor.next();	
			       Result result = null;
			       if(entry.getValue().equals(ChangeType.NOTIFICATION))
						result = doSendViaGcm("Neue Änderungen auf dem Vertretungsplan", sender, sub, coll);
			       else if (entry.getValue().equals(ChangeType.NO_NOTIFICATION))
						result = doSendViaGcm("NO_NOTIFICATION", sender, sub, coll);
			       if(result != null && result.getErrorCodeName() != null) {
			    	   message.append("        GCM Fehler: " + result.getErrorCodeName());
			       }
			       i++;
			   }
			} finally {
			   cursor.close();
			}
			
			message.append("   " + entry.getKey() + ": " + i + " Nachrichten versandt" + "\n");
		}
		if (changedClasses.size() > 0) {
			BasicDBObject query = new BasicDBObject("schoolId", schoolId);
			query.append("klasse", "Alle");
			DBCursor cursor = coll.find(query);
			
			int i = 0;
			try {
			   while(cursor.hasNext()) {
			       DBObject sub = cursor.next();	
			       Result result = null;
			       if(changedClasses.containsValue(ChangeType.NOTIFICATION))
						result = doSendViaGcm("Neue Änderungen auf dem Vertretungsplan", sender, sub, coll);
			       else if (changedClasses.containsValue(ChangeType.NO_NOTIFICATION))
						result = doSendViaGcm("NO_NOTIFICATION", sender, sub, coll);
			       if(result != null && result.getErrorCodeName() != null) {
			    	   message.append("        GCM Fehler: " + result.getErrorCodeName());
			       }
			       i++;
			   }
			} finally {
			   cursor.close();
			}
			
			message.append("   Alle: " + i + " Nachrichten versandt" + "\n");
		}
		return message.toString();
	}
	
	public enum ChangeType {
		NOTIFICATION, NO_NOTIFICATION
	}
	
	
	
	/**
	 * Sends the message using the Sender object to the registered device.
	 * 
	 * @param message
	 *            the message to be sent in the GCM ping to the device.
	 * @param sender
	 *            the Sender object to be used for ping,
	 * @param deviceInfo
	 *            the registration id of the device.
	 * @return Result the result of the ping.
	 */
	private static Result doSendViaGcm(String message, Sender sender,
			DBObject deviceInfo, DBCollection coll) throws IOException {
		// Trim message if needed.
		if (message.length() > 1000) {
			message = message.substring(0, 1000) + "[...]";
		}

		// This message object is a Google Cloud Messaging object, it is NOT 
		// related to the MessageData class
		Message msg = new Message.Builder().addData("message", message).build();
		Result result = sender.send(msg, (String) deviceInfo.get("_id"),
				5);
		if (result.getMessageId() != null) {
			String canonicalRegId = result.getCanonicalRegistrationId();
			if (canonicalRegId != null) {
				// same device has more than on registration ID: update database
				coll.save(deviceInfo);
			}
		} else {
			String error = result.getErrorCodeName();
			if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				// application has been removed from device - unregister database
				coll.remove(deviceInfo);
			}
		}

		return result;
	}
}
