````markdown
# 📚 Study Buddy

> **A modern JavaFX-based Academic Resource Sharing & Student Management Platform**

![Java](https://img.shields.io/badge/Java-17+-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-blue)
![SQL Server](https://img.shields.io/badge/Database-Microsoft%20SQL%20Server-red)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![License](https://img.shields.io/badge/License-MIT-green)

Study Buddy is a desktop application built with **JavaFX** and **Microsoft SQL Server** that helps students organize their academic life in one place. The platform enables users to upload and share study materials, manage notes and tasks, ask academic questions, and collaborate with fellow students while providing administrators with powerful moderation and management tools.

Developed as a **Bachelor of Computer Application (BCA)** academic project, Study Buddy follows modern software engineering practices using the **MVC architecture**, **DAO pattern**, and **Service Layer** for maintainability and scalability.

---

# 📖 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Security Features](#security-features)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Configuration](#database-configuration)
- [Running the Application](#running-the-application)
- [Admin Panel](#admin-panel)
- [Application Workflow](#application-workflow)
- [Modules](#modules)
- [Learning Outcomes](#learning-outcomes)
- [Screenshots](#screenshots)
- [Contributing](#contributing)
- [License](#license)

---

# 📖 Overview

Study Buddy provides a centralized platform where students can:

- Store and organize academic notes
- Upload and download study resources
- Ask and answer academic questions
- Track personal study tasks
- Receive notifications
- Manage their academic profile

Administrators can monitor platform activity, approve uploaded content, manage users, and maintain the overall quality of shared resources through a dedicated admin dashboard.

---

# ✨ Features

## 👨‍🎓 Student Features

- 🔐 Secure User Registration & Login
- 🔑 Password Hashing
- 🏠 Dashboard Overview
- 📝 Create & Manage Personal Notes
- 📂 Upload Academic Resources
- 📥 Download Resources
- ❓ Ask & Browse Questions (Q&A)
- ⭐ Search & Filter Notes/Resources
- 📚 Department, Semester & Subject Filtering
- 👤 Profile Management
- ✅ Personal Task Tracking
- 🔔 Notifications
- 📜 Activity History

---

## 👨‍💼 Admin Features

- 🔑 Separate Admin Login
- 📊 Dashboard Analytics
- 👥 User Management
- 📝 Notes Moderation
- 📂 Resource Moderation
- ❓ Question Moderation
- ✅ Approve / Reject Uploaded Content
- 🏫 Department Management
- 📚 Semester & Subject Management
- 📈 Reports & Activity Logs
- 🔔 Notifications
- ⚙️ System Settings

---

# 🔒 Security Features

- Password Hashing
- Session Management
- Role-Based Access Control
- Admin Approval Workflow
- Input Validation
- SQL Injection Protection
- Secure JDBC Database Connection

---

# 🛠️ Tech Stack

| Layer | Technology |
|---------|------------|
| Language | Java 17+ |
| UI Framework | JavaFX (FXML + CSS) |
| Database | Microsoft SQL Server |
| JDBC Driver | Microsoft JDBC Driver for SQL Server |
| Build Tool | Maven |
| Architecture | MVC + DAO + Service Layer |
| IDE | IntelliJ IDEA (Recommended) |

---

# 🏗️ System Architecture

The project follows the **Model-View-Controller (MVC)** architecture.

```
Presentation Layer (JavaFX + FXML)
                │
        Controller Layer
                │
        Service Layer
                │
           DAO Layer
                │
 Microsoft SQL Server
```

This layered architecture improves maintainability, scalability, and code organization.

---

# 📂 Project Structure

```text
StudyBuddy/
├── pom.xml
└── src/main/
    ├── java/com/studybuddy/
    │   ├── App.java
    │   ├── Main.java
    │   ├── controllers/
    │   ├── services/
    │   ├── dao/
    │   ├── models/
    │   ├── utils/
    │   └── admin/
    │       ├── controllers/
    │       ├── dao/
    │       ├── services/
    │       ├── utils/
    │       ├── AdminApp.java
    │       └── AdminMain.java
    └── resources/com/studybuddy/
        ├── fxml/
        ├── css/
        ├── admin/fxml/
        ├── admin/css/
        └── images/
```

---

# 🗄️ Database

**Database Name:** `StudyBuddy`

Main database entities include:

- Users
- Departments
- Semesters
- Subjects
- Notes
- Resources
- Questions
- Tasks
- Notifications
- Activity Logs
- Reports
- Admin Accounts

---

# 📋 Prerequisites

Before running the application, ensure you have:

- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8+
- Microsoft SQL Server
- SQL Server Authentication enabled
- IntelliJ IDEA (Recommended)

Maven automatically downloads all required dependencies, including JavaFX and the Microsoft SQL Server JDBC driver.

---

# 🚀 Installation

## 1. Clone Repository

```bash
git clone https://github.com/Bachelor-Of-Computer-Application/Study-Buddy-.git
cd Study-Buddy-/StudyBuddy
```

---

## 2. Build Project

```bash
mvn clean install
```

---

## 3. Open in IntelliJ IDEA

Open the project as a Maven project and allow IntelliJ to import dependencies automatically.

---

# ⚙️ Database Configuration

Configure your SQL Server connection in:

```
src/main/java/com/studybuddy/dao/DatabaseConnection.java
```

Example configuration:

```java
DB_URL      = "jdbc:sqlserver://localhost:1433;databaseName=StudyBuddy;encrypt=true;trustServerCertificate=true;";
DB_USER     = "studybuddy";
DB_PASSWORD = "StudyBuddy123";
```

### Database Setup

1. Create a database named **StudyBuddy**
2. Create the SQL login:

```sql
CREATE LOGIN studybuddy
WITH PASSWORD='StudyBuddy123';

CREATE USER studybuddy
FOR LOGIN studybuddy;

ALTER ROLE db_owner
ADD MEMBER studybuddy;
```

3. Enable:
   - SQL Server Authentication
   - TCP/IP
   - Port 1433

> **Security Note:** Hardcoded credentials are intended only for local development. Store credentials securely using environment variables or configuration files for production deployments.

---

# ▶️ Running the Application

## User Application

```bash
mvn javafx:run
```

or run:

```
App.java
```

from IntelliJ IDEA.

---

## Admin Application

Run:

```bash
mvn javafx:run -Padmin
```

or execute:

```
AdminApp.java
```

directly from your IDE.

---

# 🔄 Application Workflow

### Student Workflow

```text
Register
    ↓
Login
    ↓
Dashboard
    ↓
Upload Note / Resource
    ↓
Pending Admin Approval
    ↓
Approved by Admin
    ↓
Visible to All Students
```

---

### Admin Workflow

```text
Login
    ↓
Dashboard
    ↓
Review Pending Content
    ↓
Approve / Reject
    ↓
Monitor Platform Activity
```

---

# 📦 Modules

- Authentication
- User Management
- Profile Management
- Notes Management
- Resource Management
- Questions & Answers
- Task Management
- Notifications
- Dashboard
- Search & Filters
- Reports
- Activity Logs
- Admin Management
- Approval System

---

# 📚 Learning Outcomes

This project demonstrates practical knowledge of:

- Object-Oriented Programming (OOP)
- JavaFX Desktop Development
- MVC Architecture
- DAO Pattern
- Service Layer Architecture
- JDBC
- Microsoft SQL Server
- CRUD Operations
- Authentication & Authorization
- Software Engineering Principles

---

# 📸 Screenshots

Add screenshots of your application here.

Suggested screenshots:

```
screenshots/
├── Login.png
├── Register.png
├── Dashboard.png
├── Notes.png
├── Resources.png
├── Tasks.png
├── Questions.png
├── Profile.png
├── Notifications.png
├── AdminDashboard.png
├── UserManagement.png
└── Reports.png
```

---

# 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch

```bash
git checkout -b feature/your-feature
```

3. Commit your changes

```bash
git commit -m "Add your feature"
```

4. Push to your branch

```bash
git push origin feature/your-feature
```

5. Open a Pull Request

---

# 👨‍💻 Author

**Prasid Gautam**

Bachelor of Computer Application (BCA)

Pokhara University

GitHub:
https://github.com/Bachelor-Of-Computer-Application

---

# 📜 License

This project was developed as part of a **Bachelor of Computer Application (BCA)** coursework project.

You may use, modify, and extend this project for educational and learning purposes.

---

# ⭐ Support

If you found this project helpful:

⭐ Star the repository

🍴 Fork the repository

📢 Share it with others

---

## 📧 Contact

For suggestions, improvements, or collaborations, feel free to reach out through GitHub.

---

<div align="center">

### 📚 Study Buddy

**Empowering Students Through Collaborative Learning**

Made with ❤️, ☕, JavaFX & Microsoft SQL Server

</div>
````
