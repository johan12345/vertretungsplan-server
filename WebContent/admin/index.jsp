<%@ page contentType="text/html; charset=utf-8" language="java" errorPage="" %>
<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="com.mongodb.*" %>
<%@ page import="com.johan.vertretungsplan.backend.*" %>
<%@ page import="com.johan.vertretungsplan.objects.*" %>
<jsp:include page="head.jsp" />
		<div class="col-sm-3 col-md-2 sidebar">
          <ul class="nav nav-sidebar">
            <li class="active"><a href="#schulen">Schulen</a></li>
            <li><a href="#logs">Logs</a></li>
          </ul>
        </div>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
          <h1 class="page-header">Dashboard</h1>

          <h2 id="schulen" class="sub-header">Schulen</h2>
          <div class="table-responsive">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Stadt</th>
                  <th>Nutzerzahl</th>
                  <th></th>
                </tr>
              </thead>
              <% 	try {
				  		List<Schule> schools = GetSchoolsServlet.getSchools();	
						MongoClient client = DBManager.getInstance();
						DB db = client.getDB("vertretungsplan"); 
						DBCollection regColl = db.getCollection("registrations");					
			  %>
              <tbody>
              	<% 	for(Schule school:schools) {
						BasicDBObject sub = new BasicDBObject("schoolId", school.getId());
						DBCursor cursor = regColl.find(sub);
						int userCount = cursor.count(); %>
                <tr>
                  <td><%= school.getId() %></td>
                  <td><%= school.getName() %></td>
                  <td><%= school.getCity() %></td>
                  <td><%= userCount %></td>
                  <td>
                      <button type="button" class="btn btn-default edit" data-toggle="modal" data-target="#editor" id="<%= school.getId() %>">
      					<span class="glyphicon glyphicon-pencil"></span>
    				  </button>
                      <button type="button" class="btn btn-default delete" id="<%= school.getId() %>">
      					<span class="glyphicon glyphicon-trash"></span>
    				  </button>
                  </td>
                </tr>
                <% }
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}%>
                <tr>
                  <td><a data-toggle="modal" data-target="#editor" id="new"><span class="glyphicon glyphicon-plus"></span> Hinzufügen</a></td>
                  <td></td>
                  <td></td>
                  <td></td>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <h2 id="logs" class="sub-header">Logs</h2>
            <div class="panel panel-default">
              <div class="panel-heading">Filtern</div>
              <div class="panel-body">
                <label class="checkbox-inline" for="errors-only">
                  <input type="checkbox" name="errors-only" id="errors-only" value="true" checked>
                  Nur Fehler
                </label>
              </div>
            </div>
          <div class="table-responsive">
            <table id="logs-table" class="table table-striped">
              <thead>
                <tr>
                  <th></th>
                  <th>Datum</th>
                  <th>Uhrzeit</th>
                  <th>Fehler</th>
                  <th>Ladezeit</th>
                </tr>
              </thead>
              <tbody>
                  <tr id="more-logs">
                      <td></td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td><a id="more-logs">mehr...</a></td>
                  </tr>
              </tbody>
            </table>
        </div>
        
        <div class="modal fade" id="editor" tabindex="-1" role="dialog" aria-labelledby="editorLabel" aria-hidden="true">
          <div class="modal-dialog modal-lg">
            <div class="modal-content">
              <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Schließen</span></button>
                <h4 class="modal-title" id="editorLabel">Modal title</h4>
              </div>
              <div class="modal-body">
              	<form class="form-horizontal">
				<fieldset>
                <!-- Text input-->
                <div class="form-group">
                  <label class="col-md-4 control-label" for="id">Schul-ID</label>  
                  <div class="col-md-8">
                    <input type="text" name="id" id="id" class="form-control input-md"/>
                  </div>
                </div>
                <!-- Textarea -->
                <div class="form-group">
  				  <label class="col-md-4 control-label" for="editor-text">JSON-Daten</label>
  				  <div class="col-md-8">
                    <div id="editor-text" class="form-control" style="height:500px" name="editor-text"></div>
                  </div>
                </div>
                <div class="form-group">
                	<div class="col-md-4"></div>
                    <div class="col-md-8">
                		<button type="button" class="btn btn-primary ladda-button" data-style="expand-right" id="check">Prüfen</button>
                    </div>
                </div>
                <div><pre id="check-result"></pre></div>
                </fieldset>
				</form>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Abbrechen</button>
                <button type="button" class="btn btn-primary ladda-button" data-style="expand-right" id="save">Speichern</button>
              </div>
            </div>
          </div>
        </div>
        
         <div class="modal fade" id="error" tabindex="-1" role="dialog" aria-labelledby="errorLabel" aria-hidden="true">
          <div class="modal-dialog modal-lg">
            <div class="modal-content">
              <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Schließen</span></button>
                <h4 class="modal-title" id="errorLabel">Fehler</h4>
              </div>
              <div class="modal-body">
              	<pre id="error-stacktrace"></pre>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-primary" data-dismiss="modal">Schließen</button>
              </div>
            </div>
          </div>
        </div>
        
        <script src="//cdn.jsdelivr.net/ace/1.1.5/min/ace.js" type="text/javascript" charset="utf-8"></script>
        <script type="text/javascript" src="js/editor.js"></script>
        <script type="text/javascript" src="js/logs.js"></script>
<jsp:include page="foot.jsp" />