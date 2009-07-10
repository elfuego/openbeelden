<%@ page session="false" %><% response.setStatus(503); 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html>
<head>
  <title>503 temporarily unavailable - Open Images</title>
  <%
  String url = (String) request.getAttribute("javax.servlet.forward.request_uri"); 
  String q = (String) request.getAttribute("javax.servlet.forward.query_string"); 
  if (url == null) {
     // mainly for debugging of this page itself
     url = request.getContextPath() + request.getServletPath();
     q   = request.getQueryString();
  }
  if (q != null) url += "?" + q;
  %>
  
  <% 
  if (request.getMethod().equals("GET")) {
    response.setHeader("Refresh", "30; url=" + url);
  }
  %>
  <%@ include file="meta.jsp" %>
</head>
<body class="error">
  <%@ include file="header.jsp" %>
  <div class="main-column">
    <h1>503 This web-site is temporary unavailable</h1>
    <h2><%= org.mmbase.Version.get() %></h2>
    <p>
      <em><%= new java.util.Date() %></em> - This web-site is currently unavailable. 
      <% if (request.getMethod().equals("GET")) { %>
         Please come <a href="<%=org.mmbase.util.transformers.Xml.XMLAttributeEscape(url)%>">back</a> in a few minutes.
      <% } else { %>
         Please press reload in a few minutes.
      <% } %>
    </p>
    <p>
      If you are the administrator of this site, and this message does not disappear, please check
      the mmbase log, which probably contains an indication of the reason why MMBase is not yet
      successfully started.
    </p>
  </div>
  <%@ include file="footer.jsp" %>
</body>
</html>

