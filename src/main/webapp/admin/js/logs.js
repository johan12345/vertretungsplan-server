function clearLogs() {
	$("#logs-table tbody tr:not(#more-logs)").remove();
	$('[data-spy="scroll"]').each(function () {
	  var $spy = $(this).scrollspy('refresh')
	})
}

function loadLogs(page, errorsOnly) {
	$.getJSON("logs?page=" + page + "&errorsOnly=" + errorsOnly, function(data) {
		$.each(data, function (index, entry) {
			var date = moment(entry.date.$date);
			if (entry.hasErrors) {
				var errorsContent = "";
				$.each(entry.results, function(index, result) {
					if(result.result == "error") {
						errorsContent += '<a href="#" class="error-link" data-content="'
							+ result.stack_trace + '">' + result.id + "</a> ";
					}
				});
			}
			
			var content = "<tr>";
			content += "<td>" + (entry.hasErrors ? '<span class="glyphicon glyphicon-remove"></span>' : '<span class="glyphicon glyphicon-ok"></span>') + "</td>";
			content += "<td>" + date.format('L') + "</td>";
			content += "<td>" + date.format('LT') + "</td>";
			content += "<td>" + (entry.hasErrors ? errorsContent : 'keine') + "</td>"
			content += "<td>" + (parseFloat(Math.round(entry.time_elapsed * 100) / 100).toFixed(2)) + "</td>"
			content += "</tr>";
			$("#more-logs").before(content);
		});
		$(".error-link").click(function() {
			$("#error").modal('show');
			$("#error-stacktrace").text($(this).attr("data-content"));
		});
		if($("#logs-table tbody tr:not(#more-logs)").length == 0) {
			var content = "<tr>";
			content += "<td></td>";
			content += "<td></td>";
			content += "<td></td>";
			content += "<td>keine Einträge</td>"
			content += "<td></td>";
			content += "</tr>";
			$("#more-logs").before(content);
		}
		$('[data-spy="scroll"]').each(function () {
		  var $spy = $(this).scrollspy('refresh')
		})
	});
}

$(function() {
	$("#error").modal({ show:false });
	
	var page = 0;
		
	clearLogs();
	loadLogs(page, $("#errors-only").prop('checked'));
	
	$("#errors-only").change(function(){
		clearLogs();
		page = 0;
        loadLogs(page, $(this).prop('checked'));
    });
	
	$("#more-logs").click(function() {
		page++;
		loadLogs(page, $("#errors-only").prop('checked'));
	});

	$("#refresh-logs").click(function() {
		page = 0;
		clearLogs();
        loadLogs(page, $("#errors-only").prop('checked'));
	})

});