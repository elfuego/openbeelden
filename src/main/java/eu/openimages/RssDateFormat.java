/*

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

*/

package eu.openimages;

import java.text.*;
import java.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Simple class to call from within a xsl-transformation to make reformatting dates easier.
 * For example for dates en times which are rather ugly in RSS because they are rfc 822 formatted.
 *
 * @author Andr\U00e9 vanToly &lt;andre@toly.nl&gt;
 * @version $Id$
 */
public final class RssDateFormat {
    private static final Logger log = Logging.getLoggerInstance(RssDateFormat.class);

    /**
     * Reformats a date and time, presumes the US locale of a RSS date.
     *
     * @param   datetime        which should be reformatted
     * @param   inputformat     for example 'Sun, 27 Jan 2008 21:00:00 EST'
     * @param   outputformat    the desired format
     * @return  a date and/or time reformatted
     */
    public static String reformatDate(String datetime, String inputformat, String outputformat) {
        String newdate = "";
        DateFormat idf = new SimpleDateFormat(inputformat, Locale.US);
        DateFormat odf = org.mmbase.util.DateFormats.getInstance(outputformat, null, org.mmbase.bridge.util.CloudThreadLocal.currentCloud().getLocale());
        if (log.isDebugEnabled()) {
            log.debug("Trying to reformat: " + datetime + ", with '" + inputformat + "' and '" + outputformat + "'");
        }
        try {
            Date date = idf.parse(datetime);
            newdate = odf.format(date);
        } catch (ParseException pe) {
            log.error("ParseException with '" + inputformat + "': " + pe);
        }

        return newdate;
    }

}
