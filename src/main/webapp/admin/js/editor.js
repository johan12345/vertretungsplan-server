$(function() {	
	$('.main').scrollspy({ target: '.sidebar', offset:80 });

	var editor = ace.edit("editor-text");
    editor.setTheme("ace/theme/monokai");
    editor.getSession().setMode("ace/mode/json");
	editor.setFontSize("14px");
	
	//NEU
	$("#new").click(function() {
		$("#editorLabel").text("Neue Schule");
		var base = {"name":"","city":"","api":"","geo":[],"data":{},"additional_info":[""]};
		editor.setValue(JSON.stringify(base, null, '\t'));
		$("#id").val("");
		$("#id").focus();
		$("#check-result").css("display", "none");
		
		$("#check").click(function() {
			$.ajax({
				type: "POST",
				url: "check",
				data: JSON.stringify($.parseJSON(editor.getValue())),
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
				data: JSON.stringify($.parseJSON(editor.getValue())),
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
	  editor.setValue("Laden...");
	  $("#check-result").css("display", "none");
	  $.ajax({
			url: "schools?id=" + $(this).attr("id")
		}).done(function( msg ) {
			editor.setValue(JSON.stringify($.parseJSON(msg), null, '\t'));
			$("#check").click(function() {
				$.ajax({
					type: "POST",
					url: "check",
					data: JSON.stringify($.parseJSON(editor.getValue())),
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
					data: JSON.stringify($.parseJSON(editor.getValue())),
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