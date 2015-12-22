Open Images Platform
====================
 
Open Images Platform Web Application - Nederlands Instituut voor Beeld &amp; Geluid / The Netherlands Institute for Sound and Vision. The Open Images Platform is a Java webapplication to manage and publish open media.  
http://www.openimages.eu  
http://www.openbeelden.nl  

Installation instructions can be found in INSTALL in this same directory.

Open Images is a platform to upload, edit and publish video, audio and images. It transcodes every media format it recognizes to an open format like OGG or PNG. 
Based on MMBase it aims to offer online access to a selection of archive material to stimulate creative reuse, including remixing of archive material in new works. Open Images also supports interlinking with other data sources (like Wikipedia), allowing the easy creation of mash ups. Access to the content is based on the Creative Commons model. The projects 'open' nature is underscored by adapting open formats and using open source software for its infrastructure.
Its first release dates January 20, 2010
    

CHECKOUT SOURCES FROM GIT
-------------------------
Clone this repo with:
```bash
$ git clone git@github.com:beeldengeluid/openbeelden.git
```

MAVEN 2 BUILD
-------------
You can build it with Maven 2. Standing in this directory (or the one you just checked out):
```bash
$ mvn clean install
```

RUN IT (JETTY)
--------------
You can run it with Jetty. Change 'jetty-env.xml' to correctly refer to your database and:
```bash   
$ mvn jetty:run
```
Point your webbrowser at http://localhost:8080.


INSTALLATION
------------
Rather then running the web application in Jetty, like above which is great for 
development purposes but not when you need persintant data, you can install it in 
Apache Tomcat or some other Java web applicaion server. Refer to our INSTALL 
document for more information about how to do this.


LICENSE
-------

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2009 Netherlands Institute for Sound and Vision

The Open Images Platform is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Open Images Platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with The Open Images Platform.  If not, see <http://www.gnu.org/licenses/>.
