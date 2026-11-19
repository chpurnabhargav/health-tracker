# Health Tracker

A personal Android app for getting fit with structure: daily weigh-ins with a trend line, calories,
water, steps and sleep, a weekly review, and a pause screen that appears when you open a food
delivery app.

Everything stays on the phone. There's no account, no server and no ads.

## Features

- **Today**: birthday countdown with your goal and projection, weight vs 7-day trend, calorie ring,
  protein, water buttons, steps and sleep, and a weekly waist check
- **Weight**: trend chart (tap or drag to inspect a day), weekly rate, BMI, projected weight on goal day,
  waist history, milestones and a list of recent weigh-ins
- **Food**: built-in list of about 100 Indian and everyday foods, your saved foods, custom foods,
  7-day calorie chart, protein/carbs/fat split, top calorie sources and late-night eating
- **Weekly review**: headline, key numbers, water by day, food-app pauses, what it noticed, and one focus
  for next week
- **Plan**: profile, goal, targets, reminders, food-app pause settings, phone setup checklist,
  backup and restore
- **Reminders**: water through the day, morning weigh-in (type your weight straight into the
  notification), meal logging, and a Sunday review
- **Food-app pause**: when you open Swiggy, Zomato or any app you choose, a full-screen message such as
  "No Bhargav, not till your birthday." appears. Three strictness levels:
  - **Gentle**: wait, then continue
  - **Strict** (default): the wait grows each time you order in a day (10 s, 30 s, 1 min, 2 min), you type a
    promise, and you log the order size before the app opens
  - **Lockdown**: no ordering except your weekly planned meal or a 5-minute emergency unlock

  An allowance only lasts while you stay in that app. Switching apps or locking the phone ends it, and the
  pause comes back when you unlock with a food app still open. The app warns you if the pause is switched off
  and shows the money saved each time you back out
- **Night lockdown**: the pause switches to Lockdown by itself during the hours you choose (9 PM to 6 AM by
  default), and optionally all weekend
- **Craving SOS**: a 5-minute guided routine (water, slow breathing, moving, mint) that ends with "Did the
  craving pass?" and smart snacks under 200 kcal if you're really hungry. Open it from Today or straight from
  the pause screen
- **Home-screen widget**: the water glass, today's litres, the birthday countdown and a one-tap Drank button
- **Water alerts**: a full-screen alert that wakes a locked phone, a 12-second card on top of whatever app
  you're using, and a high-priority notification with +250 ml and snooze buttons
- **Built for consistency**: minimum-day mode, "never miss twice", weekly averages instead of daily
  pass/fail, calorie target recalculated every 5 kg, a new goal that starts automatically after the
  goal date, and an optional planned meal out each week

## Design

Dark "night neon" theme with Space Grotesk and Inter (both SIL Open Font License, see `third_party/fonts`).
The goal countdown is a 3D glass orb that fills with liquid as you lose weight; the liquid and the water glass stay
level when you tilt the phone. Cards tilt in 3D when pressed, numbers count up, and confetti marks your water
goal and lower weigh-ins.

## Install on a Samsung phone

1. Download `app-release.apk` to the phone.
2. Samsung's **Auto Blocker** stops apps from outside the Play Store. Turn it off for the install:
   **Settings → Security and privacy → Auto Blocker → off**.
3. Open the APK from **My Files → Downloads** and tap **Install**. If asked, allow
   **Install unknown apps** for My Files. If Play Protect warns about an unknown app, choose
   **More details → Install anyway**.
4. Turn Auto Blocker back on. The installed app keeps working.
5. Open the app and follow the setup. In **Plan → Phone setup**, turn on:
   - **Notifications**
   - **Run in the background**, so Samsung doesn't put the app to sleep and delay reminders
   - **Food-app pause**: first **Settings → Apps → Health Tracker → ⋮ → Allow restricted settings**,
     then **Settings → Accessibility → Installed apps → Health Tracker food-app pause → on**
   - **Steps and sleep** through Health Connect. In Samsung Health, also turn on
     **Settings → Health Connect** syncing so steps and sleep reach the app

Before installing a new version, use **Plan → Save a backup**.

## Build

Requirements: JDK 17 or newer and the Android SDK (platform 36, build-tools 35).

```bash
./gradlew :app:assembleRelease
```

Without a `keystore.properties` file, the release build is signed with the debug key. To sign with your
own key, create `keystore.properties` in the project root:

```
storeFile=keystore/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Updates only install over an existing copy when they're signed with the same key, so keep the keystore safe.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

`CalcTest` checks the calorie maths, trend line, birthday countdown, milestones and reminder timing.
`ScreenshotTest` loads three weeks of sample data, checks the numbers the screens show, and renders every
screen to `app/build/outputs/roborazzi/` with Robolectric and Roborazzi.

## How it works

- Kotlin, Jetpack Compose and Material 3; Room for the database and DataStore for settings
- Calorie target: Mifflin-St Jeor resting energy × activity level, minus 550, 825 or 1,100 kcal a day for
  a gentle, steady or fast pace, never below 1,500 (men) or 1,200 (women)
- Trend weight: the average of all weigh-ins in the 7 days ending that day
- Reminders use exact alarms and are rescheduled after a restart
- The food-app pause is an accessibility service that only looks at which app comes to the front.
  It can't read screen content (`canRetrieveWindowContent` is off)

This app gives general guidance, not medical advice. Check with a doctor before starting a diet or
exercise plan.
