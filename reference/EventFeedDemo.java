/* Copyright (c) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nilriri.LunaCalendar.tools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.nilriri.LunaCalendar.dao.ScheduleBean;
import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;
import org.nilriri.LunaCalendar.dao.Constants.Schedule;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.google.gdata.client.Query;
import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchStatus;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.data.calendar.WebContent;
import com.google.gdata.data.extensions.ExtendedProperty;
import com.google.gdata.data.extensions.Recurrence;
import com.google.gdata.data.extensions.Reminder;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Reminder.Method;
import com.google.gdata.util.ServiceException;

/**
 * Demonstrates basic Calendar Data API operations on the event feed using the
 * Java client library:
 * 
 * <ul>
 * <li>Retrieving the list of all the user's calendars</li>
 * <li>Retrieving all events on a single calendar</li>
 * <li>Performing a full-text query on a calendar</li>
 * <li>Performing a date-range query on a calendar</li>
 * <li>Creating a single-occurrence event</li>
 * <li>Creating a recurring event</li>
 * <li>Creating a quick add event</li>
 * <li>Creating a web content event</li>
 * <li>Updating events</li>
 * <li>Adding reminders and extended properties</li>
 * <li>Deleting events via batch request</li>
 * </ul>
 
 
 
¡á  EventEntry entry = new EventEntry(); 
 
    entry.Title.Text = "testing from .NET"; 
    entry.Content.Content = "testing from .NET"; 
 
    String recurData = 
   "DTSTART;TZID=America/Los_Angeles:20071202T080000\r\n" + 
   "DTEND;TZID=America/Los_Angeles:20071202T090000\r\n" + 
   "RRULE:FREQ=WEEKLY;WKST=SU;UNTIL=20071230T160000Z;BYDAY=SU\r\n"; 
 
   Recurrence recurrence = new Recurrence(); 
   recurrence.Value = recurData; 
   entry.Recurrence = recurrence; 
 
   Reminder reminder = new Reminder(); 
   reminder.Minutes = 15; 
   reminder.Method = Reminder.ReminderMethod.all;    
   entry.Reminders.Add(reminder); 


Uri postUri = new Uri("http://www.google.com/calendar/feeds/default/private/full"); 
EventEntry createdEntry = (EventEntry) service.Insert(postUri, myEntry); 
 
 
 
 
 */
public class EventFeedDemo {

    // The base URL for a user's calendar metafeed (needs a username appended).
    private static final String METAFEED_URL_BASE = "http://www.google.com/calendar/feeds/";

    // The string to add to the user's metafeedUrl to access the event feed for
    // their primary calendar.
    private static final String EVENT_FEED_URL_SUFFIX = "/private/full";

    // The URL for the metafeed of the specified user.
    // (e.g. http://www.google.com/feeds/calendar/jdoe@gmail.com)
    private static URL metafeedUrl = null;

    // The URL for the event feed of the specified user's primary calendar.
    // (e.g. http://www.googe.com/feeds/calendar/jdoe@gmail.com/private/full)
    private static URL eventFeedUrl = null;

    /**
     * Prints a list of all the user's calendars.
     * 
     * @param service An authenticated CalendarService object.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server
     */
    private static void printUserCalendars(CalendarService service) throws IOException, ServiceException {
        // Send the request and receive the response:
        CalendarFeed resultFeed = service.getFeed(metafeedUrl, CalendarFeed.class);

        Log.e("Calendar", "Your calendars:");
        Log.e("Calendar", "");
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            CalendarEntry entry = resultFeed.getEntries().get(i);
            Log.e("Calendar", "\t" + entry.getTitle().getPlainText());
        }
        Log.e("Calendar", "");
    }

    /**
     * Prints the titles of all events on the calendar specified by
     * {@code feedUri}.
     * 
     * @param service An authenticated CalendarService object.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static void printAllEvents(CalendarService service) throws ServiceException, IOException {
        // Send the request and receive the response:
        CalendarEventFeed resultFeed = service.getFeed(eventFeedUrl, CalendarEventFeed.class);

        Log.e("Calendar", "All events on your calendar:");
        Log.e("Calendar", "");
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            CalendarEventEntry entry = resultFeed.getEntries().get(i);
            Log.e("Calendar", "\t" + entry.getTitle().getPlainText());
        }
        Log.e("Calendar", "");
    }

    /**
     * Prints the titles of all events matching a full-text query.
     * 
     * @param service An authenticated CalendarService object.
     * @param query The text for which to query.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static void fullTextQuery(CalendarService service, String query) throws ServiceException, IOException {
        Query myQuery = new Query(eventFeedUrl);
        myQuery.setFullTextQuery("Tennis");

        CalendarEventFeed resultFeed = service.query(myQuery, CalendarEventFeed.class);

        Log.e("Calendar", "Events matching " + query + ":");
        Log.e("Calendar", "");
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {
            CalendarEventEntry entry = resultFeed.getEntries().get(i);
            Log.e("Calendar", "\t" + entry.getTitle().getPlainText());
        }
        Log.e("Calendar", "");
    }

    /**
     * Prints the titles of all events in a specified date/time range.
     * 
     * @param service An authenticated CalendarService object.
     * @param startTime Start time (inclusive) of events to print.
     * @param endTime End time (exclusive) of events to print.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static void dateRangeQuery(Context context, CalendarService service, DateTime startTime, DateTime endTime) throws ServiceException, IOException {

        CalendarQuery myQuery = new CalendarQuery(eventFeedUrl);
        myQuery.setMinimumStartTime(startTime);
        myQuery.setMaximumStartTime(endTime);

        Log.e("Calendar", "myQuery = " + myQuery == null ? "myQuery is null " : myQuery.toString());
        Log.e("Calendar", "service = " + service == null ? "service is null " : service.toString());

        // Send the request and receive the response:
        CalendarEventFeed resultFeed = null;
        try {
            resultFeed = service.query(myQuery, CalendarEventFeed.class);
        } catch (Exception e) {
            Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resultFeed == null)
            return;

        ScheduleDaoImpl dao = new ScheduleDaoImpl(context, null, Prefs.getSDCardUse(context));

        Log.e("Calendar", "Events from " + startTime.toString() + " to " + endTime.toString() + ":");
        for (int i = 0; i < resultFeed.getEntries().size(); i++) {

            CalendarEventEntry entry = resultFeed.getEntries().get(i);

            Log.d("Calendar", "getSequence=" + entry.getSequence());
            Log.d("Calendar", "getLocations=" + entry.getLocations().get(0).getValueString());
            Log.d("Calendar", "getPublished=" + entry.getPublished().toString());
            Log.d("Calendar", "getTimes.size=" + entry.getTimes().size());
            Log.d("Calendar", "getTimes=" + entry.getTimes().get(0).getValueString());
            Log.d("Calendar", "getIcalUID=" + entry.getIcalUID().toString());
            Log.d("Calendar", "getId=" + entry.getId());

            List<When> wList = entry.getTimes();
            /*
                        for (int j = 0; j < wList.size(); j++) {
                            Log.d("Calendar", "getTimes_" + j + "=" + ((When) wList.get(j)).getStartTime().toString());
                        }
            */
            ScheduleBean scheduleBean = new ScheduleBean();

            scheduleBean.setTitle(entry.getTitle().getPlainText());
            scheduleBean.setDate(entry.getPublished().toString().substring(0, 10));
            scheduleBean.setDate(entry.getTimes().get(0).getStartTime().toString().substring(0, 10));
            scheduleBean.setContents(entry.getPlainTextContent());
            scheduleBean.setGID(entry.getIcalUID().toString());

            dao.insert(scheduleBean);

        }

    }

    /**
     * Helper method to create either single-instance or recurring events. For
     * simplicity, some values that might normally be passed as parameters (such
     * as author name, email, etc.) are hard-coded.
     * 
     * @param service An authenticated CalendarService object.
     * @param eventTitle Title of the event to create.
     * @param eventContent Text content of the event to create.
     * @param recurData Recurrence value for the event, or null for
     *        single-instance events.
     * @param isQuickAdd True if eventContent should be interpreted as the text of
     *        a quick add event.
     * @param wc A WebContent object, or null if this is not a web content event.
     * @return The newly-created CalendarEventEntry.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */

    /*

    URL postUrl =
    new URL("http://www.google.com/calendar/feeds/jo@gmail.com/private/full");
    CalendarEventEntry myEntry = new CalendarEventEntry();

    myEntry.setTitle(new PlainTextConstruct("Tennis with Beth"));
    myEntry.setContent(new PlainTextConstruct("Meet for a quick lesson."));

    DateTime startTime = DateTime.parseDateTime("2006-04-17T15:00:00-08:00");
    DateTime endTime = DateTime.parseDateTime("2006-04-17T17:00:00-08:00");
    When eventTimes = new When();
    eventTimes.setStartTime(startTime);
    eventTimes.setEndTime(endTime);
    myEntry.addTime(eventTimes);

    // Send the request and receive the response:
    CalendarEventEntry insertedEntry = myService.insert(postUrl, myEntry);     
     
     */
    private static CalendarEventEntry createEvent(CalendarService service, String eventTitle, String eventContent, String recurData, Calendar pCalendar, boolean isQuickAdd, WebContent wc) throws ServiceException, IOException {
        CalendarEventEntry myEntry = new CalendarEventEntry();

        myEntry.setTitle(new PlainTextConstruct(eventTitle));
        myEntry.setContent(new PlainTextConstruct(eventContent));
        myEntry.setQuickAdd(isQuickAdd);
        myEntry.setWebContent(wc);

        // If a recurrence was requested, add it. Otherwise, set the
        // time (the current date and time) and duration (30 minutes)
        // of the event.
        if (recurData == null) {
            Calendar calendar = new GregorianCalendar();
            calendar.set(pCalendar.get(Calendar.YEAR), pCalendar.get(Calendar.MONTH), pCalendar.get(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);

            //String sdate = Common.fmtDate(pCalendar);
            //DateTime startTime = DateTime.parseDateTime(sdate + "T09:00:00-23:59");
            //DateTime endTime = DateTime.parseDateTime(sdate + "T09:00:00-23:59");

            DateTime startTime = new DateTime(calendar.getTime(), TimeZone.getDefault());
            calendar.add(Calendar.HOUR, 23);
            DateTime endTime = new DateTime(calendar.getTime(), TimeZone.getDefault());

            When eventTimes = new When();
            eventTimes.setStartTime(startTime);
            eventTimes.setEndTime(endTime);
            myEntry.addTime(eventTimes);

        } else {
            Recurrence recur = new Recurrence();
            recur.setValue(recurData);
            myEntry.setRecurrence(recur);
        }
        try {
            // Send the request and receive the response:
            return service.insert(eventFeedUrl, myEntry);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a single-occurrence event.
     * 
     * @param service An authenticated CalendarService object.
     * @param eventTitle Title of the event to create.
     * @param eventContent Text content of the event to create.
     * @return The newly-created CalendarEventEntry.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry createSingleEvent(CalendarService service, String eventTitle, String eventContent, Calendar pCalendar) throws ServiceException, IOException {
        return createEvent(service, eventTitle, eventContent, null, pCalendar, false, null);
    }

    /**
     * Creates a quick add event.
     * 
     * @param service An authenticated CalendarService object.
     * @param quickAddContent The quick add text, including the event title, date
     *        and time.
     * @return The newly-created CalendarEventEntry.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry createQuickAddEvent(CalendarService service, String quickAddContent, Calendar pCalendar) throws ServiceException, IOException {
        return createEvent(service, null, quickAddContent, null, pCalendar, true, null);
    }

    /**
     * Creates a web content event.
     * 
     * @param service An authenticated CalendarService object.
     * @param title The title of the web content event.
     * @param type The MIME type of the web content event, e.g. "image/gif"
     * @param url The URL of the content to display in the web content window.
     * @param icon The icon to display in the main Calendar user interface.
     * @param width The width of the web content window.
     * @param height The height of the web content window.
     * @return The newly-created CalendarEventEntry.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry createWebContentEvent(CalendarService service, String title, String type, String url, String icon, String width, String height) throws ServiceException, IOException {
        WebContent wc = new WebContent();

        wc.setHeight(height);
        wc.setWidth(width);
        wc.setTitle(title);
        wc.setType(type);
        wc.setUrl(url);
        wc.setIcon(icon);

        return createEvent(service, title, null, null, Calendar.getInstance(), false, wc);
    }

    /**
     * Creates a new recurring event.
     * 
     * @param service An authenticated CalendarService object.
     * @param eventTitle Title of the event to create.
     * @param eventContent Text content of the event to create.
     * @return The newly-created CalendarEventEntry.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry createRecurringEvent(CalendarService service, String eventTitle, String eventContent) throws ServiceException, IOException {
        // Specify a recurring event that occurs every Tuesday from May 1,
        // 2007 through September 4, 2007. Note that we are using iCal (RFC 2445)
        // syntax; see http://www.ietf.org/rfc/rfc2445.txt for more information.
        String recurData = "DTSTART;VALUE=DATE:20070501\r\n" + "DTEND;VALUE=DATE:20070502\r\n" + "RRULE:FREQ=WEEKLY;BYDAY=Tu;UNTIL=20070904\r\n";

        return createEvent(service, eventTitle, eventContent, recurData, Calendar.getInstance(), false, null);
    }

    /**
     * Updates the title of an existing calendar event.
     * 
     * @param entry The event to update.
     * @param newTitle The new title for this event.
     * @return The updated CalendarEventEntry object.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry updateTitle(CalendarEventEntry entry, String newTitle) throws ServiceException, IOException {
        entry.setTitle(new PlainTextConstruct(newTitle));
        return entry.update();
    }

    /**
     * Adds a reminder to a calendar event.
     * 
     * @param entry The event to update.
     * @param numMinutes Reminder time, in minutes.
     * @param methodType Method of notification (e.g. email, alert, sms).
     * @return The updated EventEntry object.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry addReminder(CalendarEventEntry entry, int numMinutes, Method methodType) throws ServiceException, IOException {
        Reminder reminder = new Reminder();
        reminder.setMinutes(numMinutes);
        reminder.setMethod(methodType);
        entry.getReminder().add(reminder);

        return entry.update();
    }

    /**
     * Adds an extended property to a calendar event.
     * 
     * @param entry The event to update.
     * @return The updated EventEntry object.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static CalendarEventEntry addExtendedProperty(CalendarEventEntry entry) throws ServiceException, IOException {
        // Add an extended property "id" with value 1234 to the EventEntry entry.
        // We specify the complete schema URL to avoid namespace collisions with
        // other applications that use the same property name.
        ExtendedProperty property = new ExtendedProperty();
        property.setName("http://www.example.com/schemas/2005#mycal.id");
        property.setValue("1234");

        entry.addExtension(property);

        return entry.update();
    }

    /**
     * Makes a batch request to delete all the events in the given list. If any of
     * the operations fails, the errors returned from the server are displayed.
     * The CalendarEntry objects in the list given as a parameters must be entries
     * returned from the server that contain valid edit links (for optimistic
     * concurrency to work). Note: You can add entries to a batch request for the
     * other operation types (INSERT, QUERY, and UPDATE) in the same manner as
     * shown below for DELETE operations.
     * 
     * @param service An authenticated CalendarService object.
     * @param eventsToDelete A list of CalendarEventEntry objects to delete.
     * @throws ServiceException If the service is unable to handle the request.
     * @throws IOException Error communicating with the server.
     */
    private static void deleteEvents(CalendarService service, List<CalendarEventEntry> eventsToDelete) throws ServiceException, IOException {

        // Add each item in eventsToDelete to the batch request.
        CalendarEventFeed batchRequest = new CalendarEventFeed();
        for (int i = 0; i < eventsToDelete.size(); i++) {
            CalendarEventEntry toDelete = eventsToDelete.get(i);
            // Modify the entry toDelete with batch ID and operation type.
            BatchUtils.setBatchId(toDelete, String.valueOf(i));
            BatchUtils.setBatchOperationType(toDelete, BatchOperationType.DELETE);
            batchRequest.getEntries().add(toDelete);
        }

        // Get the URL to make batch requests to
        CalendarEventFeed feed = service.getFeed(eventFeedUrl, CalendarEventFeed.class);
        Link batchLink = feed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
        URL batchUrl = new URL(batchLink.getHref());

        // Submit the batch request
        CalendarEventFeed batchResponse = service.batch(batchUrl, batchRequest);

        // Ensure that all the operations were successful.
        boolean isSuccess = true;
        for (CalendarEventEntry entry : batchResponse.getEntries()) {
            String batchId = BatchUtils.getBatchId(entry);
            if (!BatchUtils.isSuccess(entry)) {
                isSuccess = false;
                BatchStatus status = BatchUtils.getBatchStatus(entry);
                Log.d("Calendar", "\n" + batchId + " failed (" + status.getReason() + ") " + status.getContent());
            }
        }
        if (isSuccess) {
            Log.d("Calendar", "Successfully deleted all events via batch request.");
        }
    }

    /**
     * Instantiates a CalendarService object and uses the command line arguments
     * to authenticate. The CalendarService object is used to demonstrate
     * interactions with the Calendar data API's event feed.
     * 
     * @param args Must be length 2 and contain a valid username/password
     */

    public static void LoadEvents(Context context, Calendar cal) {
        CalendarService myService = new CalendarService("org.nilriri.LunaCalendar");

        String userName = Prefs.getGMailUserID(context);
        String userPassword = Prefs.getGMailPassword(context);

        // Create the necessary URL objects.
        try {
            metafeedUrl = new URL(METAFEED_URL_BASE + userName);
            eventFeedUrl = new URL(METAFEED_URL_BASE + userName + EVENT_FEED_URL_SUFFIX);
        } catch (MalformedURLException e) {
            // Bad URL
            Log.d("SYSTEM", "Uh oh - you've got an invalid URL.");
            e.printStackTrace();
            return;
        }

        try {
            myService.setUserCredentials(userName, userPassword);

            // Demonstrate various feed queries.
            Calendar start = Calendar.getInstance();
            //start.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            start.set(cal.get(Calendar.YEAR), 1, 1);
            start.setFirstDayOfWeek(Calendar.SUNDAY);

            Calendar end = Calendar.getInstance();
            end.set(cal.get(Calendar.YEAR), 12, 31);
            //end.add(Calendar.MONTH, 1);
            //end.add(Calendar.DAY_OF_MONTH, -1);

            Log.d("XX", Common.fmtDate(start));
            Log.d("YY", Common.fmtDate(end));

            dateRangeQuery(context, myService, DateTime.parseDate(Common.fmtDate(start)), DateTime.parseDate(Common.fmtDate(end)));

        } catch (IOException e) {
            // Communications error
            Log.d("XX", "There was a problem communicating with the service.");
            e.printStackTrace();
        } catch (ServiceException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();

        }

    }

    public static void addEvents(Context context, Cursor cursor) {
        CalendarService myService = new CalendarService("org.nilriri.LunaCalendar");

        String userName = Prefs.getGMailUserID(context);
        String userPassword = Prefs.getGMailPassword(context);

        try {
            metafeedUrl = new URL(METAFEED_URL_BASE + userName);
            eventFeedUrl = new URL(METAFEED_URL_BASE + userName + EVENT_FEED_URL_SUFFIX);
        } catch (MalformedURLException e) {
            Log.d("Calendar", "Uh oh - you've got an invalid URL.");
            e.printStackTrace();
            return;
        }

        try {
            myService.setUserCredentials(userName, userPassword);

            //CalendarEventEntry singleEvent = createSingleEvent(myService, "Title", "hahaha test contents...");
            if (cursor.moveToNext()) {
                Calendar cal = Calendar.getInstance();
                cal.setFirstDayOfWeek(Calendar.SUNDAY);

                String sdate = "";
                if (cursor.getInt(Schedule.COL_SCHEDULE_REPEAT) == 9) {
                    cal.set(Calendar.MONTH, cursor.getInt(Schedule.COL_DDAY_DISPLAYYN + 2) - 1);
                    cal.set(Calendar.DAY_OF_MONTH, cursor.getInt(Schedule.COL_DDAY_DISPLAYYN + 3));
                } else {
                    cal.set(cursor.getInt(Schedule.COL_DDAY_DISPLAYYN + 1), cursor.getInt(Schedule.COL_DDAY_DISPLAYYN + 2) - 1, cursor.getInt(Schedule.COL_DDAY_DISPLAYYN + 3));
                }

                CalendarEventEntry singleEvent = createSingleEvent(myService, cursor.getString(Schedule.COL_SCHEDULE_TITLE), cursor.getString(Schedule.COL_SCHEDULE_CONTENTS), cal);

            }

            //singleEvent = updateTitle(singleEvent, "Important (»õÀÏÁ¤)");

            //singleEvent = addReminder(singleEvent, 15, Method.EMAIL);

            //singleEvent = addExtendedProperty(singleEvent);

        } catch (IOException e) {
            Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show();
        } catch (ServiceException e) {
            Toast.makeText(context, "Please try again.", Toast.LENGTH_SHORT).show();
        } finally {
            cursor.close();
        }
    }

    /**
     * Prints the command line usage of this sample application.
     */
    private static void usage() {
        Log.d("Calendar", "Syntax: EventFeedDemo <username> <password>");
        Log.d("Calendar", "\nThe username and password are used for " + "authentication.  The sample application will modify the specified " + "user's calendars so you may want to use a test account.");
    }
}
