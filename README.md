# 📚 StudyBuddy

> A modern JavaFX desktop application for academic collaboration, resource sharing, and student productivity.

![Java](https://img.shields.io/badge/Java-17+-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-blue)
![SQL Server](https://img.shields.io/badge/Database-Microsoft%20SQL%20Server-red)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📖 About

**StudyBuddy** is a desktop application developed using **JavaFX** and **Microsoft SQL Server** to help students organize their academic life in one place.

The platform allows students to upload and share notes, manage study resources, ask academic questions, track personal tasks, and collaborate with peers. An integrated admin panel ensures that uploaded content is reviewed before becoming publicly available.

This project was developed as a **Bachelor of Computer Application (BCA)** academic project following modern software engineering principles.

---

## ✨ Features

### 👨‍🎓 Student Portal

- 🔐 Secure User Registration & Login
- 👤 Profile Management
- 📝 Personal Notes Management
- 📂 Upload Study Resources
- 📥 Download Approved Resources
- ❓ Questions & Answers
- ✅ Task Management
- 🔍 Search & Filters
- 🔔 Notifications
- 📊 Dashboard Overview
- 📜 Activity History

### 👨‍💼 Admin Panel

- 🔑 Separate Admin Login
- 👥 User Management
- 📝 Notes Approval
- 📂 Resource Approval
- ❓ Question Moderation
- 📊 Dashboard Analytics
- 📈 Reports
- 🔔 Notifications
- ⚙️ System Management

---

## 🛠 Tech Stack

| Technology | Description |
|------------|-------------|
| Java 17 | Programming Language |
| JavaFX | Desktop UI Framework |
| FXML | UI Layout |
| CSS | Styling |
| Microsoft SQL Server | Database |
| JDBC | Database Connectivity |
| Maven | Dependency Management |
| IntelliJ IDEA | Development IDE |

---

## 🏗 Architecture

The application follows the **MVC (Model-View-Controller)** architecture.

```
JavaFX UI
     │
Controllers
     │
Services
     │
DAO Layer
     │
Microsoft SQL Server
```

---

## 📂 Project Structure

```
StudyBuddy
│
├── src/main/java
│   ├── controllers
│   ├── dao
│   ├── services
│   ├── models
│   ├── utils
│   ├── admin
│   ├── App.java
│   └── Main.java
│
├── src/main/resources
│   ├── fxml
│   ├── css
│   ├── images
│   └── admin
│
└── pom.xml
```

---

## 🚀 Getting Started

### Prerequisites

- Java JDK 17+
- Apache Maven
- Microsoft SQL Server
- IntelliJ IDEA (Recommended)

### Clone Repository

```bash
git clone https://github.com/Bachelor-Of-Computer-Application/Study-Buddy-.git
cd Study-Buddy-/StudyBuddy
```

### Install Dependencies

```bash
mvn clean install
```

### Configure Database

Update the SQL Server connection in:

```
src/main/java/com/studybuddy/dao/DatabaseConnection.java
```

Example:

```java
DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=StudyBuddy;encrypt=true;trustServerCertificate=true;";
DB_USER = "studybuddy";
DB_PASSWORD = "StudyBuddy123";
```

Run the provided SQL script to create the database.

---

## ▶ Running the Application

### User Application

```bash
mvn javafx:run
```

### Admin Application

```bash
mvn javafx:run -Padmin
```

Or run `App.java` / `AdminApp.java` directly from IntelliJ IDEA.

---

## 🔄 Workflow

```
Student
──────────────

Register
     ↓
Login
     ↓
Upload Notes
     ↓
Pending Approval
     ↓
Admin Approval
     ↓
Available to Everyone
```

---

## 📚 Key Learning Outcomes

- JavaFX Desktop Development
- Object-Oriented Programming (OOP)
- MVC Architecture
- DAO Design Pattern
- JDBC
- Microsoft SQL Server
- CRUD Operations
- Authentication & Authorization
- Software Engineering Principles

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch

```bash
git checkout -b feature/new-feature
```

3. Commit your changes

```bash
git commit -m "Add new feature"
```

4. Push your branch

```bash
git push origin feature/new-feature
```

5. Open a Pull Request

---

## 👨‍💻 Author

**Prasid Gautam**

Bachelor of Computer Application (BCA)

Pokhara University

GitHub: https://github.com/Bachelor-Of-Computer-Application

---

## 📄 License

This project was developed for educational purposes as part of the Bachelor of Computer Application (BCA) curriculum.

---

## ⭐ Support

If you found this project useful:

⭐ Star this repository

🍴 Fork it

💡 Share your feedback

---

<div align="center">

### 📚 StudyBuddy

**Empowering Students Through Collaborative Learning**

Made with ❤️ using JavaFX & Microsoft SQL Server

</div>