"use strict";

$(document).ready(setup);
//handles events from html page
var start = 0;
var finish = 0;
var finalResult = 1000;
function setup() {
//	Width and height
	var d = new Date();
	start = d.getTime();
	var w = 500;
	var h = 500;
	var padding = 30;


	var x = d3.scale.ordinal()
	.rangeRoundBands([0, w], .1);

	var y = d3.scale.linear()
	.range([h, 0]);
	
//	Create SVG element
	var svg = d3.select("body")
	.append("svg")
	.attr("width", w)
	.attr("height", h);

	d3.csv("stresstest1000.csv", function(data, error) {

		data.forEach(function(d) {
			d.x = +d.x;
			d.y = +d.y;
			d.r = +d.r;
		});

		x.domain([0, d3.max(data, function(d) { return d.x; })]);
		y.domain([0, d3.max(data, function(d) { return d.y; })]);

//		Create scale functions
		var xScale = d3.scale.linear()
		.domain([0, d3.max(data, function(d) { return d.x; })])
		.range([padding, w - padding * 2]);

		var yScale = d3.scale.linear()
		.domain([0, d3.max(data, function(d) { return d.y; })])
		.range([h - padding, padding]);

		var rScale = d3.scale.linear()
		.domain([0, d3.max(data, function(d) { return d.y; })])
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
		.data(data)
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
		finalResult = start - d.getTime();
		$("#time").text(finalResult);
	});
}