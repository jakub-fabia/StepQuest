# StepQuest

Android native app for yearly step goal tracking using Health Connect and Recording API, with a home screen widget and CSV import/export.

## Features

- **Step Dashboard** — daily, weekly, and monthly step counts with progress toward a configurable yearly goal
- **Daily Step Log** — browse all recorded step data by date
- **Health Connect Integration** — syncs step data from Health Connect and the Recording API
- **Home Screen Widget** — at-a-glance progress for today, 7-day, and 30-day periods
- **CSV Import/Export** — bulk import or export step data in `date,steps` format

## Tech Stack

- Kotlin
- MVVM architecture (Fragment / ViewModel / Repository)
- Room database
- Health Connect API & Recording API
- XML layouts
- Min SDK 34 / Target SDK 36

## Build

```bash
JAVA_HOME=/path/to/jbr ./gradlew assembleDebug
```

## Project Structure

```
com.example.stepquest
├── data/
│   ├── local/          # Room DB, DAO, entities, preferences
│   ├── repository/     # StepsRepository
│   └── source/         # HealthConnect, RecordingApi, CSV data sources
├── domain/model/       # Domain models & mappers
├── ui/
│   ├── dashboard/      # Main step counter dashboard
│   ├── dailysteps/     # Daily step log view
│   └── stepslist/      # Step list view
└── widget/             # Home screen widget (StepsWidgetProvider)
```

