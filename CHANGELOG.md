# Changelog

## 1.1.1

Small fixes and interactions on the weather strip and calendar.

- Tap a day in the month grid to open it in the device's Calendar app.
- Tap the weather strip to open a full forecast page for the current city, with correct units
  and time format for that location.
- The month grid now refreshes automatically at local midnight instead of waiting for another
  trigger to notice the day has changed.
- Larger month grid text and more spacing before the events list.

## 1.1.0

Adds a weather strip and an alarm hand on the clock face.

- Weather strip: today's condition and temperature plus a two-day forecast, from Open-Meteo, for
  a location picked once in Settings via city search. No location permission, no Play Services.
  Refreshes hourly and caches the last successful result for offline viewing.
- Fourth clock hand showing the next system alarm when one is set within 12 hours.

## 1.0.0

An analog clock in the style of the Braun BC12 alarm clock, sharing one screen with a
current-month calendar and a configurable RSS headline carousel.

- Custom-drawn clock face with a ticking second hand and light/dark palettes that follow the
  system theme (or a manual override).
- Month-grid calendar backed by the device's synced calendars, with a dot on days that have
  events and a list of today's and upcoming events. Prompts for calendar access when it isn't
  granted.
- RSS/Atom headline carousel: fetches configured feeds, caches the last successful result for
  offline viewing, and opens the tapped headline's link in a browser.
- Settings screen for managing feeds, choosing the theme (System / Light / Dark), and
  refreshing feeds on demand.
- Portrait and landscape layouts; the screen stays on while the app is in the foreground.
- Signed, R8-minified release build.
