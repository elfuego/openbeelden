<%@ page contentType="application/json;charset=utf-8" 
%><%@ page session="false" 
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@ taglib uri="http://www.opensymphony.com/oscache" prefix="os"
%><%@ taglib tagdir="/WEB-INF/tags/oip" prefix="oip"
%><mm:content 
    type="application/json" expires="0" postprocessor="reducespace" language="${param.locale}">
  <%-- version: '$Id$' --%>
  <jsp:directive.page session="false" />
  <jsp:directive.page import="org.mmbase.bridge.*" />
  <% response.setHeader("Access-Control-Allow-Origin", "*"); %>
  <mm:cloud>  

    <mm:import externid="set">openimages</mm:import>
    <mm:import externid="lang">nl</mm:import>
    
    <mm:import externid="max">100</mm:import>
    <mm:import externid="offset">0</mm:import>
    
    
    <mm:escaper type="tagstripper" id="tagstripper" />
    
    <c:choose>
      <c:when test="${!empty set}">
        
        <mm:listnodescontainer type="properties">
          <mm:constraint field="key" value="oai-set" operator="EQUAL" />
          <mm:constraint field="value" value="${set}" operator="EQUAL" />
          <mm:maxnumber value="1" />
          <mm:listnodes>
            <mm:field name="parent" id="portal" write="false" />
          </mm:listnodes>
        </mm:listnodescontainer>

        <mm:import id="analyzer" reset="true">org.apache.lucene.analysis.KeywordAnalyzer</mm:import>
        <mm:import id="value">media* <oip:portalquery portal="${set eq 'openimages' ? 'pool_oip' : portal}" /></mm:import>
        <mm:import id="fields" reset="true">indexId</mm:import>

      </c:when>
      <c:otherwise>
        <!-- default portal, display latest -->
        <mm:import id="value">media*</mm:import>
        <mm:import id="fields">indexId</mm:import>
      </c:otherwise>
    </c:choose>

    <mm:import id="index">media</mm:import>
    <mm:import id="extraconstraints"></mm:import>
    <mm:import id="sortfields">REVERSE:lastmodified</mm:import>
    <mm:import id="filter">
      <c:if test="${!empty start}">lastmodified:GTE:<mm:time format="yyyyMMddHHmm" time="$start" />00</c:if>
      <c:if test="${!empty end}">${!empty start ? '+ ' : ''}lastmodified:LTE:<mm:time format="yyyyMMddHHmm" time="$end" />00</c:if>
    </mm:import>

    <mm:nodelistfunction 
        module="lucene"
        id="list"
        name="search"
        referids="index,value,offset,max,fields?,sortfields?,filter?,analyzer?,extraconstraints" />

    <mm:function
        module="lucene"
        id="total"
        name="searchsize"
        write="false"
        referids="index,value,fields?,filter?,analyzer?,extraconstraints" />
    
{
  "repository" : "Open Image Platform",
  "admin-email" : "info@openimage.eu",
  "base-url" : "<mm:url page="/" absolute="true" />",
  "size" : ${total}, 
  "media" : [
    <mm:listnodes referid="list">
      <mm:nodeinfo type="type" id="type" write="false" />
      {
        "number" : "${_node.number}",
        "identifier" : "oai:openimages.eu:${_node.number}",
        "title" : "<mm:field name="title" escape="js-double-quotes" />",
        "language" : "${_node.language}",
        "subtitle" : "${_node.subtitle}",
        "keywords" : [<mm:field name="keywords" escape="none"><mm:isnotempty>
            <c:forEach items="${fn:split(_, ';')}" var="subject" varStatus="st">
              "<c:out value="${fn:trim(subject)}" />"<c:if test="${st.last ne true}">,</c:if>
            </c:forEach>
        </mm:isnotempty></mm:field>],
        "intro" : "<mm:field name="intro" escape="tagstripper,text/xml" />",
        "body" : "<mm:field name="body" escape="tagstripper,text/xml" />",
        "creator" : "${_node.creator}",
        "publisher" : "${_node.publisher}",
        "contributor" : [<mm:field name="contributor" escape="none"><mm:isnotempty>
          <c:forEach items="${fn:split(_, ';')}" var="key" varStatus="st">
            "<c:out value="${fn:trim(key)}" />"<c:if test="${st.last ne true}">,</c:if>
          </c:forEach>
        </mm:isnotempty></mm:field>],
        "date" : "<mm:field name="date"><mm:time format="yyyy-MM-dd" /></mm:field>",
        "type" : "<c:choose><c:when test="${fn:startsWith(type,'image')}">image</c:when><c:when test="${fn:startsWith(type,'audio')}">audio</c:when><c:otherwise>video</c:otherwise></c:choose>",
        "length" : "${_node.length}",
        "urls" : [
        <mm:functioncontainer>
          <mm:param name="format">WEBM,OGV,MP4,OGG,TS,M3U8,MP3,OGG,OGA,PNG,JPG,JPEG,MPG,MPEG</mm:param>
          <mm:listfunction name="filteredurls" varStatus="st">
          <%-- c:if test="${!empty _.URL and _.available}" --%>
            <mm:node number="${_.source.number}">
            <c:choose>
              <c:when test="${fn:startsWith(type,'image')}">
                {
                  "format" : "${_.state eq 'SOURCE' ? 'source' : 'image'}",
                  "url" : "<mm:url page="${_.URL}" absolute="true" />"
                }<c:if test="${st.last ne true}">,</c:if>
              </c:when>
              <c:when test="${!empty _node.label}">
                {
                  "format" : "${_node.label}",
                  "url" : "<mm:url page="${_.URL}" absolute="true" />"
                }<c:if test="${st.last ne true}">,</c:if>
              </c:when>
              <c:otherwise>
                {
                  "format" : "unknown",
                  "url" : "<mm:url page="${_.URL}" absolute="true" />"
                }<c:if test="${st.last ne true}">,</c:if>
              </c:otherwise>
            </c:choose>
            </mm:node>
          <%-- /c:if --%>
          </mm:listfunction>
        </mm:functioncontainer>
        ],
        <c:if test="${fn:startsWith(type, 'video')}"><mm:nodefunction name="thumbnail">
        "thumb" : "<mm:image template="s(512x288)+size(512x288>)" absolute="true" />",
        </mm:nodefunction></c:if>
        <c:if test="${fn:startsWith(type, 'audio')}">
          <mm:relatednodescontainer type="images" role="related" searchdirs="destination">
            <mm:maxnumber value="1" />
            <mm:relatednodes>
              "thumb" : "<mm:image template="s(512x288)+size(512x288>)" absolute="true" />",
            </mm:relatednodes>
          </mm:relatednodescontainer>
        </c:if>
        "coverage" : [<mm:field name="coverage" escape="none"><mm:isnotempty>
          <c:forEach items="${fn:split(_, ';')}" var="coverage" varStatus="st">
            "<c:out value="${fn:trim(coverage)}" />"<c:if test="${st.last ne true}">,</c:if>
          </c:forEach>
        </mm:isnotempty></mm:field>]
      
      }<mm:last inverse="true">,</mm:last>
    </mm:listnodes>
  ]
}    
  </mm:cloud>
</mm:content>