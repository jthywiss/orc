{-
PLEASE READ THE FOLLOWING BEFORE RUNNING

You will need a Google Calendar account, and you
will need popup blockers disabled.

This program will:
1. Ask you for temporary permission to update your
   Google Calendar
2. Search MySpace for bands performing in Austin TX
3. Find perormances by those bands
4. Add those performances to your Google Calendar
   under the calendar "Orc Music Calendar"

We make use of a technology called "OAuth" to
access your Google Calendar without your username
and password.  When you run the program, you will
be prompted to log in to Google directly and then
grant this program permission to update your
calendar.  You may revoke this permission at any
time through your "My Account" page on Google.

We offer no warranty or guarantees that running
this program will not completely trash your Google
Calendar.  This demo is intended only for those
with a sense of adventure.
-}

include "net.inc"

-- imports
site MySpace = orc.lib.music_calendar.MySpace
site GoogleCalendarFactory = orc.lib.music_calendar.GoogleCalendar

-- declarations
val oauth = OAuthProvider("orc/orchard/oauth.properties")
val Google = GoogleSearchFactory("orc/orchard/google.properties")
val GoogleCalendar = GoogleCalendarFactory(oauth, "google")
val phrases =
    "site:www.myspace.com 'Austin, TX' 'Band Members'"
  | "site:www.myspace.com 'Austin, Texas' 'Band Members'"

-- execution
(
    GoogleCalendar.authenticate() 
  | println("Authenticating...") >> stop
) >>
phrases >phrase>
Google(phrase) >pages>
each(pages) >page>
each(page()) >result>
println("Scraping " + result.url) >>
MySpace.scrapeMusicShows(result.url) >musicShows>
each(musicShows) >musicShow>
GoogleCalendar.addMusicShow(musicShow) >>
stop
; "DONE"
