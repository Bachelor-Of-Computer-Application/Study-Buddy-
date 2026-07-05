# Study Buddy 📚

Study Buddy is a desktop application built with **JavaFX** and **Microsoft SQL Server** that helps students organize their academic life in one place — notes, tasks, questions/answers, and study resources — with a dedicated admin panel for platform moderation.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Database Configuration](#database-configuration)
- [Running the Application](#running-the-application)
- [Admin Panel](#admin-panel)
- [Contributing](#contributing)
- [License](#license)

## Features

**User App**
- 🔐 User registration and login with hashed passwords
- 🏠 Home dashboard with an overview of activity
- 📝 Create, view, and manage personal notes
- ❓ Ask and browse questions (Q&A / peer help)
- 📂 Access and share study resources
- 👤 Profile management
- ✅ Task tracking

**Admin Panel**
- 🔑 Separate admin login
- 📊 Dashboard overview with platform statistics
- 👥 User moderation
- 📝 Notes, questions, and resources moderation
- 📈 Reports and activity logs
- 🔔 Notifications
- ⚙️ App-wide settings management

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX (FXML + CSS) |
| Language | Java |
| Database | Microsoft SQL Server |
| DB Driver | Microsoft JDBC Driver for SQL Server (`sqljdbc_13.4`) |
| Architecture | MVC-style (Controllers, Services, DAO, Models) |
| IDE | IntelliJ IDEA |

## Project Structure

```
StudyBuddy/
├── src/main/java/com/studybuddy/
│   ├── App.java                  # JavaFX Application entry point
│   ├── Main.java                 # Launcher (delegates to App)
│   ├── controllers/              # UI controllers (Login, Dashboard, Notes, etc.)
│   ├── services/                 # Business logic layer
│   ├── dao/                      # Data access objects (JDBC)
│   ├── models/                   # Data models (User, Note, Task, Question, etc.)
│   ├── utils/                    # Helpers (SceneManager, PasswordHasher, SessionManager, etc.)
│   └── admin/                    # Admin panel (controllers, dao, services, app entry)
│       ├── controllers/
│       ├── dao/
│       ├── services/
│       ├── utils/
│       ├── AdminApp.java
│       └── AdminMain.java
└── src/main/resources/com/studybuddy/
    ├── fxml/                     # FXML layouts (user app)
    ├── css/                      # Stylesheets (user app)
    ├── admin/fxml/                # FXML layouts (admin panel)
    ├── admin/css/                 # Stylesheets (admin panel)
    └── images/                    # App icons and images
```

## Prerequisites

Before running Study Buddy, make sure you have:

1. **Java Development Kit (JDK)** — version 17 or higher
2. **JavaFX SDK 26.0.1** — [Download here](https://gluonhq.com/products/javafx/)
3. **Microsoft SQL Server** (local instance or remote) with **SQL Server Authentication** enabled
4. **Microsoft JDBC Driver for SQL Server** (`sqljdbc_13.4`) — [Download here](https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server)
5. **IntelliJ IDEA** (recommended) or another IDE with JavaFX support

> **Note:** This project currently uses manually attached libraries (IntelliJ project libraries) rather than a build tool like Maven or Gradle. The JavaFX SDK and JDBC driver jars are expected to live outside the project folder as referenced in the `.idea/libraries` configuration.

## Setup & Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Bachelor-Of-Computer-Application/Study-Buddy-.git
   cd Study-Buddy-
   ```

2. **Download and place the dependencies**
   - Download the **JavaFX SDK 26.0.1** and **JavaFX jmods 26.0.1**
   - Download the **Microsoft JDBC Driver (sqljdbc_13.4)**
   - Place them somewhere accessible on your machine (e.g. a `java/` folder alongside the project) and update the library paths in IntelliJ (`File > Project Structure > Libraries`) to point to:
     - `javafx-sdk-26.0.1/lib`
     - `javafx-jmods-26.0.1`
     - `sqljdbc_13.4/enu/jars`

3. **Open the project in IntelliJ IDEA**
   - Open the `StudyBuddy` folder as a project
   - Let IntelliJ index the sources under `src/main/java` and resources under `src/main/resources`

4. **Configure VM options for JavaFX** (if running outside a modular setup), add to your Run Configuration:
   ```
   --module-path "<path-to-javafx-sdk>/lib" --add-modules javafx.controls,javafx.fxml
   ```

## Database Configuration

Study Buddy connects to SQL Server using the following default settings (found in `DatabaseConnection.java`):

```java
DB_URL      = "jdbc:sqlserver://localhost:1433;databaseName=StudyBuddy;encrypt=true;trustServerCertificate=true;"
DB_USER     = "studybuddy"
DB_PASSWORD = "StudyBuddy123"
```

To set up your database:

1. Create a database named `StudyBuddy` on your SQL Server instance.
2. Create a SQL login (or update the credentials above to match an existing login):
   ```sql
   CREATE LOGIN studybuddy WITH PASSWORD = 'StudyBuddy123';
   CREATE USER studybuddy FOR LOGIN studybuddy;
   ALTER ROLE db_owner ADD MEMBER studybuddy;
   ```
3. Ensure **SQL Server Authentication** and **TCP/IP** are enabled (via SQL Server Configuration Manager) and that the server is listening on port `1433`.
4. Update `DB_URL`, `DB_USER`, and `DB_PASSWORD` in `src/main/java/com/studybuddy/dao/DatabaseConnection.java` if your setup differs.

> ⚠️ **Security note:** Credentials are currently hardcoded for local development. For any shared or production use, move these to environment variables or a config file excluded from version control.

## Running the Application

**From IntelliJ:**
- Run `App.java` (or `Main.java`) located at `src/main/java/com/studybuddy/App.java`

**From the command line** (adjust paths to your local JavaFX SDK and dependency jars):
```bash
javac --module-path "<path-to-javafx-sdk>/lib" --add-modules javafx.controls,javafx.fxml \
  -cp "<path-to-jdbc-driver>.jar" \
  -d out $(find src/main/java -name "*.java")

java --module-path "<path-to-javafx-sdk>/lib" --add-modules javafx.controls,javafx.fxml \
  -cp "out:<path-to-jdbc-driver>.jar:src/main/resources" \
  com.studybuddy.App
```

If the database connection fails on startup, the app will still launch (the connection error is caught and logged) but data-dependent features won't work until the database is reachable.

## Admin Panel

The admin panel is a separate JavaFX entry point:
- Entry class: `com.studybuddy.admin.AdminMain` / `AdminApp.java`
- Run this class directly (same VM options as above) to launch the admin login screen and dashboard.

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

## License

This project is developed as part of a Bachelor of Computer Application (BCA) coursework project. Add your preferred license here (e.g. MIT) if you intend to distribute it publicly.

---

Made with ☕ and JavaFX by the Study Buddy team.