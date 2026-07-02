# 📚 StudyBuddy (Student Application)

StudyBuddy is a JavaFX desktop application designed to help students organize their academic activities through note management, resource sharing, question discussions, and personal study tracking.

## ✨ Features

### 🔐 Authentication
- User Registration
- User Login
- Secure Logout

### 📊 Dashboard
- Personalized welcome screen
- Study statistics overview
- Recent activity
- Quick action shortcuts
- Progress tracking

### 📝 Notes
- Create, edit, and delete personal notes
- Search and organize notes
- Community Notes section
- Note approval workflow (UI)

### 📂 Resources
- Browse study resources
- Upload resources (UI)
- Preview and download resources
- Search and filter resources

### ❓ Questions
- Ask academic questions
- Browse question feed
- View question details
- Answer discussions (UI)
- Search and filter questions

### 👤 Profile
The Profile page includes:

- Overview
- Edit Profile (integrated into Profile page)
- Account Settings
- Password
- Study Interests
- Notifications
- Activity
- Achievements

> **Note:** The Edit Profile functionality is integrated directly into the Profile page and is no longer a separate page.

---

## 🏗️ Architecture

The application follows the MVC architecture with separate Service and DAO layers.

```text
JavaFX (FXML + CSS)
        │
        ▼
   Controllers
        │
        ▼
    Services
        │
        ▼
       DAO
        │
        ▼
 Microsoft SQL Server
```

---

## 🧭 Application Flow

```text
Launch Application
        │
        ▼
 Login / Register
        │
        ▼
      Home
        │
        ▼
 Sidebar Navigation
        │
 ┌──────┼──────────┬─────────────┬─────────────┬─────────────┐
 ▼      ▼          ▼             ▼             ▼             ▼
Dashboard Notes  Resources   Questions      Profile      Logout
```

The Home page acts as the main container, dynamically loading each module into the center content area without opening additional windows.

---

## 🎨 User Interface

- Modern academic design
- Sidebar navigation
- Responsive layouts
- Rounded cards
- Consistent styling
- Reusable CSS components
- JavaFX Charts

---

## 🚀 Tech Stack

- Java
- JavaFX 26
- FXML
- CSS
- Microsoft SQL Server
- MVC + Service + DAO Architecture

---

> **Current Status:** Student application UI, navigation, and module structure are actively being improved. Database integration and backend enhancements will be completed in future updates.
