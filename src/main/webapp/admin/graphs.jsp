<HTML>
<HEAD>
	<TITLE>Grafik</TITLE>
	<%@ page import="javax.servlet.http.HttpUtils,java.util.Enumeration" %>
	<%@ page import="java.util.*" %>
    <%@ page import="java.text.*" %>
    <%@ page import="com.mongodb.*" %>
    <%@ page import="com.johan.vertretungsplan.backend.*" %>
    
    <%  MongoClient client = DBManager.getInstance();
		DB db = client.getDB("vertretungsplan");	
		
		DBCollection coll = db.getCollection("logs");		
		DBCursor logs = coll.find();
		
		DBObject order = new BasicDBObject("_id", -1);
		logs.sort(order); %>
        
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
      google.load("visualization", "1.1", {packages:["annotationchart"]});
      google.setOnLoadCallback(drawChart);
      function drawChart() {
        var data = google.visualization.arrayToDataTable([
			['Zeit', 'Ladezeit'],
          <% int i = 0;
			 while(logs.hasNext() && i < 100000) {
				DBObject log = logs.next();
				double timeElapsed = (Double) log.get("time_elapsed");
				long time = ((Date) log.get("date")).getTime();%>
				  [new Date(<%= time %>),  <%= timeElapsed %>],
		  <% i++; } %>
        ]);

        var options = {
          title: 'Ladezeit'
        };

        var chart = new google.visualization.AnnotationChart(document.getElementById('chart_div'));
        chart.draw(data, options);
      }
    </script>
</HEAD>
<BODY>

<div id="chart_div" style="width: 900px; height: 500px;"></div>

</BODY>
</HTML>

