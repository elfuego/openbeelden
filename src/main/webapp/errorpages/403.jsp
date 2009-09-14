<%@ page session="false" %><% response.setStatus(403); 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html>
<head>
    <title>403 Forbidden</title>
    <%@ include file="meta.jsp" %>   
  </head>
<body class="error">
  <%@ include file="header.jsp" %>
  <div class="main-column">
    <h2>403 Forbidden - <%= request.getAttribute("javax.servlet.error.message") %></h2>
    <h3><%= org.mmbase.Version.get()%></h3>
    <p>
      &nbsp;
      <% String mesg = (String) request.getAttribute("org.mmbase.servlet.error.message");
         if (mesg != null) {
       %>
      <%= mesg %>
      <% } %>
    </p>
  </div>
  <%@ include file="footer.jsp" %>
</body>
</html>
