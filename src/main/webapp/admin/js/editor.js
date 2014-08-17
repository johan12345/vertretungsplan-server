$(function() {		
	//NEU
	$("#new").click(function() {
		$("#editorLabel").text("Neue Schule");
		var base = {"name":"","city":"","api":"","geo":[],"data":{},"additional_info":[""]};
		$("#editor-text").val(JSON.stringify(base, null, 2));
		$("#id").val("");
		$("#id").focus();
		$("#check-result").css("display", "none");
		
		$("#check").click(function() {
			$.ajax({
				type: "POST",
				url: "check",
				data: JSON.stringify($.parseJSON($("#editor-text").val())),
				contentType: "application/json; charset=utf-8",
				success: function( msg ) {
					$("#check-result").text(JSON.stringify($.parseJSON(msg), null, 2));
					$("#check-result").css("display", "block");
				},
				failure: function( msg ) {alert( "Error" + msg );}
			});
		});
		
		$("#save").click(function() {
			$.ajax({
				type: "POST",
				url: "schools?id=" + $("#id").val(),
				data: JSON.stringify($.parseJSON($("#editor-text").val())),
				contentType: "application/json; charset=utf-8",
				success: function( msg ) {location.reload();},
				failure: function( msg ) {alert( "Error" + msg );}
			});
		});
	});
	//BEARBEITEN
	$(".edit").click(function() {
	  $("#editorLabel").text("Schule bearbeiten");
	  $("#id").val($(this).attr("id"));
	  $("#editor-text").text("Laden...");
	  $("#check-result").css("display", "none");
	  $.ajax({
			url: "schools?id=" + $(this).attr("id")
		}).done(function( msg ) {
			$("#editor-text").val(JSON.stringify($.parseJSON(msg), null, 2));
			$("#check").click(function() {
				$.ajax({
					type: "POST",
					url: "check",
					data: JSON.stringify($.parseJSON($("#editor-text").val())),
					contentType: "application/json; charset=utf-8",
					success: function( msg ) {
						$("#check-result").text(JSON.stringify($.parseJSON(msg), null, 2));
						$("#check-result").css("display", "block");
					},
					failure: function( msg ) {alert( "Error" + msg );}
				});
			});
			$("#save").click(function() {
				$.ajax({
					type: "POST",
					url: "schools?id=" + $("#id").val() + "&overwrite=yes",
					data: JSON.stringify($.parseJSON($("#editor-text").val())),
					contentType: "application/json; charset=utf-8",
					success: function( msg ) {location.reload();},
					failure: function( msg ) {alert( "Error" + msg );}
				});
			});
	}).fail(function( xhr, msg ) {
		alert( "Error: " + msg )
	});
	});
	//LÖSCHEN
	$(".delete").click(function() {
	$.ajax({
		type: "DELETE",
		url: "schools?id=" + $("#id").val(),
		success: function( msg ) {location.reload();},
		failure: function( msg ) {alert( "Error" + msg );}
	});
	});
});