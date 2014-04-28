"use strict";

var paramWidth = 400;
var paramHeight = 500;

$(document).ready(setup);
//handles events from html page

var selections;
var currentTab;

function setup() {
	var info = data.getColumns();
	selections = jQuery.parseJSON(info);
	populateSettings();
}

function populateSettings() {
	currentTab = "selectgraph";
	$("#selectgraph").hide();
	$('#title').html("Select a graph type");
	$("#selectgraph").append($(createTitle("Graph Type")));
	$("#selectgraph").append($(createItem("Bar Graph", "graph")));
	$("#selectgraph").append($(createItem("Line Graph", "graph")));
	$("#selectgraph").append($(createItem("Scatter Plot", "graph")));
	$("#selectgraph").append($(createItem("Load Graph", "load")));
	$($("#selectgraph").children()).hide();
	rollDownNewOptions($("#selectgraph"));
}

function populateXSettings() {
	if($("#selectx").children().length == 0) {
		$("#selectx").hide();
		currentTab = "selectx";
		$('#title').html("Select an x axis");
		$("#selectx").empty();
		$("#selectx").append($(createTitle("X axis")));
		for(var k in selections) {
			$("#selectx").append($(createItem(k, selections[k])));
		}
		$($("#selectx").children()).hide();
		manageInvalidInputs();
		rollDownNewOptions($("#selectx"));
	}
}

function populateYSettings() {	
	if($("#selecty").children().length == 0) {
		$('#selecty').hide();
		currentTab = "selecty";
		$('#title').html("Select a y axis");
		$("#selecty").empty();
		$("#selecty").append($(createTitle("Y axis")));
		for(var k in selections) {
			$("#selecty").append($(createItem(k, selections[k])));
		}
		$($("#selecty").children()).hide();
		manageInvalidInputs();
		rollDownNewOptions($("#selecty"));	
	}
}

function rollDownNewOptions(form) {
	form.show();
	form.children(".title").show();
	form.children(":not(.invalid)").slideDown('slow', function() {});
}

function createTitle(title) {
	var div = document.createElement("div");
	$(div).addClass("title");
	$(div).append("<p class=\"label\">" + title + "</p>");
	return div;
}

function deactivateTitle(title) {
	$(title).animate({
		fontSize: '18pt',
	}, 200, function() {});
}

function activateTitle(title) {
	$(title).animate({
		fontSize: '26pt',
	}, 200, function() {});
}

function createItem(text, type) {
	var div = document.createElement("div");
	$(div).addClass("listing");
	$(div).addClass(type);
	$(div).addClass("unselected");
	$(div).attr('id', text);
	$(div).append("<p class=\"label\">" + text + "</p>");
	$(div).toggle(function() {
		//closing file
		if($("#title").hasClass("edit_button")) {
			$("#title").html("Edit");
		}
		currentTab = "none";
		$(div).removeClass("unselected");
		$(div).addClass("selected");
		$(div).siblings(".unselected").slideUp('slow');
		$(div).promise().done(function() {
			$(div).siblings(".unselected").removeClass("previous");
			$(div).animate({
				fontSize: '14pt',
			}, 200, function() {});
			deactivateTitle($(div).siblings(".title"));
			if(!$(div).hasClass("previous")) {
				dispatchNextFolder($(div).parent().attr('id'), $(div).attr('id'));
			}
		});
		
	}, function() {
		//opening file
		//if the tab being opened is not the current tab, select a default
		//value in that tab and open the new one.
		$(div).animate({
			fontSize: '24pt',
		}, 200, function() {
			$(div).siblings(":not(.invalid)").slideDown('slow');
			if(currentTab != "none" && currentTab != $(div).parent().attr('id')) {
				if($("#" + currentTab).find('.previous').length > 0) {
					$("#" + currentTab).find('.previous').click();
				}
				else {
					var noneDiv = $(createItem("none", currentTab));
					$("#" + currentTab).children(".title").after(noneDiv);
					$(noneDiv).addClass("none");
					$(noneDiv).click();
				}
			}
			currentTab = $(div).parent().attr('id');
			selectTitle();
			if($(div).hasClass("none")) {
				$(div).remove();
			}
		});
		activateTitle($(div).siblings(".title"));
		$(div).removeClass("selected");
		$(div).addClass("unselected");
		$(div).addClass("previous");
	});
	return div;
}

function selectTitle() {
	if(currentTab == "selectgraph") {
		$('#title').html("Select a graph type");
	}
	else if(currentTab == "selectx") {
		$('#title').html("Select an x axis");
	}
	else if(currentTab == "selecty") {
		$('#title').html("Select a y axis");
	}
}

function dispatchNextFolder(form, folder) {
	if(folder != "none") {
		if(folder == "Bar Graph" || folder == "Scatter Plot") {
			populateXSettings();
		}
		else if(form == "selectx") {
			populateYSettings();
		}
		else if(form == "selecty") {
			packageEditMenu();
		}
		manageInvalidInputs();
	}
	if($("#title").hasClass("edit_button")) {
		$("#title").promise().done(function() {
			selectGraph();
		});
	}
}

function packageEditMenu() {
	if(!$("#title").hasClass("edit_button")) {
		$("#title").html("Edit");
		$("#title").addClass("edit_button");
		$("#title").toggle(function() {
			$("#title").html("Edit");
			$("#title").siblings().slideUp('slow', function() {});
		},
		function() {
			$("#title").html("Edit");
			$("#title").siblings().slideDown('slow', function() {});
		});
		$("#title").click();
	}
}

function selectGraph() {
	$('#svg_body').html("");
	var graphType = $("#selectgraph").children(".selected")[0].id;
	if(graphType == "Bar Graph") {
		drawGraph(getData());
	}
	else if(graphType == "Scatter Plot") {
		drawScatter(getScatterData());
	}
}

function manageInvalidInputs() {
	$(".invalid").removeClass("invalid");
	if($("#selectgraph").children(".selected")[0].id == "Bar Graph") {
		$("#selecty").children(':not(.Number, .title)').addClass("invalid");
	}
	else if($("#selectgraph").children(".selected")[0].id == "Scatter Plot") {
		$("#selectx").children(':not(.Number, .title)').addClass("invalid");
		$("#selecty").children(':not(.Number, .title)').addClass("invalid");
	}
}

function getScatterData() {
	var xString = $('#selectx').children('.selected')[0].id;
	var yString = $('#selecty').children('.selected')[0].id;
	//var paramWidth = 0;
	//var paramHeight = 0;
	var names = JSON.parse(data.getColumnData(xString));
	var values = JSON.parse(data.getColumnData(yString));
	var dataJ = new Array();
	for(var j = 0; j < names.length; j++) {
		var mm = ((names[j]+values[j])/100) + 8;
		dataJ[j] = {x:names[j], y:values[j], r:mm};
	}
	return dataJ;
}

function drawScatter(dataJ) {
//	Width and height
	var w = paramWidth;
	var h = paramHeight;
	var padding = 30;


	var x = d3.scale.ordinal()
	.rangeRoundBands([0, w], .1);

	var y = d3.scale.linear()
	.range([h, 0]);
	
//	Create SVG element
	var svg = d3.select("#svg_body")
	.append("svg")
	.attr("width", w)
	.attr("height", h);
	
	dataJ.forEach(function(d) {
		d.x = +d.x;
		d.y = +d.y;
		d.r = +d.r;
	});

	x.domain([0, d3.max(dataJ, function(d) { return d.x; })]);
	y.domain([0, d3.max(dataJ, function(d) { return d.y; })]);

//		Create scale functions
	var xScale = d3.scale.linear()
	.domain([0, d3.max(dataJ, function(d) { return d.x; })])
	.range([padding, w - padding * 2]);

	var yScale = d3.scale.linear()
	.domain([0, d3.max(dataJ, function(d) { return d.y; })])
	.range([h - padding, padding]);

	var rScale = d3.scale.linear()
	.domain([0, d3.max(dataJ, function(d) { return d.y; })])
	.range([2, 5]);

//		Define X axis
	var xAxis = d3.svg.axis()
	.scale(xScale)
	.orient("bottom")
	.ticks(5);

//		Define Y axis
	var yAxis = d3.svg.axis()
	.scale(yScale)
	.orient("left")
	.ticks(5);

//		Create circles
	svg.selectAll("circle")
	.data(dataJ)
	.enter()
	.append("circle")
	.attr("cx", function(d) {
		return xScale(d.x);
	})
	.attr("cy", function(d) {
		return yScale(d.y);
	})
	.attr("r", function(d) {
		return rScale(d.r);
	})
	.attr("fill", function(d) { if(d.r > 80){
		return "red";
	} else if (d.r > 40){
		return "teal";
	} else {
		return "black";
	}});

//		Create X axis
	svg.append("g")
	.attr("class", "axis")
	.attr("transform", "translate(0," + (h - padding) + ")")
	.call(xAxis);

//		Create Y axis
	svg.append("g")
	.attr("class", "axis")
	.attr("transform", "translate(" + padding + ",0)")
	.call(yAxis);
}

function getData() {
	var xString = $('#selectx').children('.selected')[0].id;
	var yString = $('#selecty').children('.selected')[0].id;
	//var paramWidth = 0;
	//var paramHeight = 0;
	var names = JSON.parse(data.getColumnData(xString));
	var values = JSON.parse(data.getColumnData(yString));
	var dataJ = new Array();
	for(var j = 0; j < names.length; j++) {
		dataJ[j] = {x:names[j], y:values[j]};
	}
	return dataJ;
}

function drawGraph(dataJ) {
	var xString = $('#selectx').children('.selected')[0].id;
	var yString = $('#selecty').children('.selected')[0].id;
	var margin = {top: 20, right: 20, bottom: 40, left: 50},
	width = paramWidth - margin.left - margin.right,
	height = paramHeight - margin.top - margin.bottom;
	var x = d3.scale.ordinal()
	.rangeRoundBands([0, width], .1);

	var y = d3.scale.linear()
	.range([height, 0]);

	var xAxis = d3.svg.axis()
	.scale(x)
	.orient("bottom");

	var yAxis = d3.svg.axis()
	.scale(y)
	.orient("left")
	.tickSubdivide(true)

	var svg = d3.select("#svg_body").append("svg")
	.attr("id", "svgElement")
	.attr("width", width + margin.left + margin.right)
	.attr("height", height + margin.top + margin.bottom)
	.append("g")
	.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
	
	dataJ.forEach(function(d) {
		d.y = +d.y;
	});
	
	x.domain(dataJ.map(function(d) { return d.x; }));
	y.domain([0, d3.max(dataJ, function(d) { return d.y; })]);
	
	svg.append("g")
	.attr("class", "x axis")
	.attr("transform", "translate(0," + height + ")")
	.call(xAxis)
	.append("text")
	.attr("x", width/2-50)
	.attr("y", 35)
	.attr("dx", ".71em")
	.style("font-size", "1.5em")
	.style("text-anchor", "start")
	.text(xString);
	
	svg.append("g")
	.attr("class", "y_axis")
	.call(yAxis)
	.append("text")
	.attr("transform", "rotate(-90)")
	.attr("y", -35)
	.attr("x", -175)
	.style("font-size", "1.5em")
	.style("text-anchor", "end")
	.text(yString);
	
	svg.selectAll(".bar")
	.data(dataJ)
	.enter().append("rect")
	.attr("class", "bar")
	.attr("x", function(d) { return x(d.x); })
	.attr("width", x.rangeBand())
	.attr("y", function(d) { return y(d.y); })
	.attr("height", function(d) { return height - y(d.y); })
	.attr("fill", function(d) { 
		if(d.y > 80){
			return "red";
		}else{
			return "teal";
		}
	});
}