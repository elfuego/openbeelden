<%@ page language="java" contentType="text/html" 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" 
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<mm:content expires="120" type="text/html" escaper="none">
<mm:cloud>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nl">
<head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8" />
  <title>Test</title>
<style type="text/css">
  img { border: 0; }
  .grey
  {
  	background-color: #ccc;
  	padding: 4px;
  }
  .red
  {
  	background-color: #c00;
  	padding: 4px;
  }
</style>
</head>
<body>

<mm:node number="img.transparent">
  <a href="#"><mm:image mode="img" template="s(200x20!)+font(mm:fonts/DINEngschriftStd.ttf)+fill(a5a5a5)+pointsize(24)+gravity(NorthWest)+text(0,1,'MMBase')" /></a>
</mm:node>

<mm:node number="img_white">
  <a href="#"><mm:image mode="img" template="s(200x20!)+font(mm:fonts/DINEngschriftStd.ttf)+fill(a5a5a5)+pointsize(24)+gravity(NorthWest)+text(0,1,'MORE FROM THIS USER')" /></a>
</mm:node>


<div class="grey">
  <mm:node number="img_white">
    <a href="#"><mm:image mode="img" template="s(200x20!)+font(mm:fonts/DINEngschriftStd.ttf)+fill(a5a5a5)+pointsize(24)+gravity(NorthWest)+text(0,1,'FAVORITES')" /></a>
  </mm:node>
</div>
<div class="red">
  <mm:node number="img_white">
    <a href="#"><mm:image mode="img" template="s(200x20!)+font(mm:fonts/DINEngschriftStd.ttf)+fill(a5a5a5)+pointsize(24)+gravity(NorthWest)+text(0,1,'21 KARAKTERS')" /></a>
  </mm:node>
</div>


</body>
</html>
</mm:cloud>
</mm:content>
