# Prompt Launcher

A keyboard-first Android home screen. Type what you want, press Enter, and it happens. Built for phones with a physical keyboard, developed on the Unihertz Titan 2 Elite.

> **Status:** 2.0, the Daylight redesign. Sideloaded release (not on Google Play). Expect rough edges and please report them with the `bug` command.

## What's new in 2.0

Every screen has been redesigned in a calm look called **Daylight**: light, dark or auto (follows the phone), Bricolage Grotesque type and a cobalt accent. Nothing sits under the Titan 2 Elite's top-left camera.

- **Home:** the time and weather sit inside the 24-hour ring. Hours lost to distracting apps turn orange, and the current hour glows. New texts show as an accent row above your mail and next event, with two rows of large app tiles below.
- **Prompt:** start typing and home frosts over. The prompt shows what will happen, chips for Chrome / Claude / Apps and Google Tasks / Todoist, and a collapsible commands box (it can be switched off in Settings). Plain searches stay clean, with no command clutter.
- **Prompt over any app:** press the Titan's Func1 key in Chrome or anywhere else, and the prompt opens over a blurred copy of that app. Send a text or add a task, and you're back where you were. Backspace or Esc on an empty prompt closes it.
- **Texting:** `@name` shows contacts with initials; tap one to lock it in. `@@` builds a group. `email name` shows email contacts the same way.
- **Quick reply (⚡):** reads the whole conversation from Android (texts, picture messages and groups), shows your replies on the right, and refreshes while open. Replies go out through Android Messages, so Messages stays the home of every conversation. Tap ⚡ with nothing waiting and you'll see "All caught up".
- **Hub:** "Now" and "Earlier" cards. Texts you've read or answered drop out. Tapping a text opens that conversation in Android Messages.
- **Assist (`?`):** a Daylight chat that knows today's date, answers general questions, and (if you allow it) answers from your mail, calendar and texts. Answers stream in as they're written.
- **Notes, email compose, settings and onboarding** are all redesigned.

Your settings, notes and favorites carry over from 1.x. On first launch 2.0 switches you to the new look once; you can change it in Settings → Appearance.

## Commands

| Type | Does |
| --- | --- |
| `@name message` | Text someone (`@@` for a group) |
| `/name` | Call someone |
| `+task` | Add a to-do (Google Tasks or Todoist) |
| `"note` | Save a note |
| `:event` | Add a calendar event |
| `?question` | Ask Assist (Gemini) |
| `.app` | Open an app |
| `!10m` / `!!7am` | Timer / alarm |
| `go place` | Directions in Maps |
| `email name` | Write an email |
| `settings`, `calendarsetup`, `weathersetup`, `usagesetup`, `geminisetup`, `taskssetup` | Setup screens |
| `bug` | Report a problem |

Anything else is a search, sent to Chrome, Claude or your apps (pick with the chips).

## Prompt over other apps

**Titan 2 / Titan 2 Elite:** Settings → Shortcut keys → Func1 key → **PTT key**, and set the PTT app to **Prompt Launcher**. Func1 now opens the prompt over whatever app is in front; press it again (or Backspace/Esc on an empty prompt) to close.

**Other phones:** anything that can launch an app (a shortcut button, a gesture app) can open it. Launching Prompt Launcher as an app, rather than as Home, opens the prompt over what you're doing.

## No Google needed

Everything works without a Google account. Calendar uses your phone's own calendars, mail comes from your mail app's notifications, to-dos can go to Todoist, and the search chips can be switched off or set to Claude or Apps. Gmail, Google Calendar and Google Tasks connections are optional extras.

## Install

Download the APK from [Releases](https://github.com/fortaybro/PromptLauncher/releases) and install it. 2.0 installs as an update over 1.x. Then choose Prompt Launcher as your Home app.

Sideloaded apps may need **Allow restricted settings** (app info → ⋮) before Android lets you grant SMS or notification access.

## Permissions

All optional; each feature asks when you first use it.

- **Contacts, SMS (send/read):** texting from the prompt, and quick reply's conversation history.
- **Notification access:** the hub, quick reply and new-text alerts.
- **Calendar:** your next event on home, and adding events with `:`. Uses the calendars already on your phone (Samsung, Outlook, Proton, local, or a synced Google account), so no Google sign-in is needed. You can switch to a direct Google Calendar connection with `calendarsetup`.
- **Usage access, location (coarse):** the 24-hour ring and weather.
- **Phone / call log:** calls from the prompt and the hub.

## Privacy

No server, no analytics, no ads, no tracking. Everything stays on your phone except requests you set up yourself:

- Weather: approximate location to [Open-Meteo](https://open-meteo.com).
- Your own Google account (Gmail, Google Calendar, Google Tasks) only if you choose to sign in, and Todoist with your token.
- Assist sends your question to Google Gemini with **your own** API key. Mail, calendar and texts are only included if you switch them on in `geminisetup`, which lists exactly what each switch sends.

## Build it yourself

1. Open this folder in a recent Android Studio and let Gradle sync.
2. Run on your device and choose Prompt Launcher as the home app.

Build output goes to `build.nosync` (so iCloud doesn't copy build files if the project lives in a synced folder).

### Google sign-in (Gmail, Calendar, Tasks)

Google sign-in only works for apps registered in a Google Cloud project. Add an **Android OAuth client** for `com.forrest.titanlauncher` with your signing key's SHA-1.

## Credits

- Builds on [MinkLauncher OpenSource](https://f-droid.org/packages/com.katoaapps.openminilaunch/) by Katoa Apps (Apache 2.0). See `NOTICE`.
- [Bricolage Grotesque](https://github.com/ateliertriay/bricolage) and [Poppins](https://fonts.google.com/specimen/Poppins) fonts, under the SIL Open Font License 1.1.
- Weather data by [Open-Meteo](https://open-meteo.com) (CC BY 4.0).

## License

Licensed under the [Apache License 2.0](LICENSE).
