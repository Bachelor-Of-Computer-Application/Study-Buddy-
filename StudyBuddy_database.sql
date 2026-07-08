-- ============================================================================
-- StudyBuddy Database - Complete Setup & Pokhara University Curriculum
-- For: LA GRANDEE International College - Pokhara University
-- Date: 2026-07-08
-- Description: Complete database creation + official PU BCA/BBA curriculum
-- ============================================================================

CREATE DATABASE StudyBuddy;
GO

USE StudyBuddy;
GO

SET NOCOUNT ON;
GO

-- ============================================================================
-- PART 1: CORE TABLES
-- ============================================================================

CREATE TABLE Users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(30) NOT NULL,
    email NVARCHAR(100) UNIQUE NOT NULL,
    password NVARCHAR(255) NOT NULL,
    role NVARCHAR(20) DEFAULT 'user',
    status NVARCHAR(20) DEFAULT 'Active',

    fullName NVARCHAR(100),
    username NVARCHAR(50),
    bio NVARCHAR(500),
    profileImagePath NVARCHAR(500),
    phoneNumber NVARCHAR(20),
    department NVARCHAR(100),
    semester NVARCHAR(20),

    preferredSubjects NVARCHAR(255),
    studyGoals NVARCHAR(500),
    learningInterests NVARCHAR(255),

    notificationsEnabled BIT DEFAULT 1,
    emailNotificationsEnabled BIT DEFAULT 1,
    resourceUpdateNotifications BIT DEFAULT 1,
    systemNotifications BIT DEFAULT 1,

    answersCount INT DEFAULT 0,
    questionsCount INT DEFAULT 0,
    achievements INT DEFAULT 0,
    points INT DEFAULT 0,

    created_at DATETIME DEFAULT GETDATE()
);
GO

CREATE TABLE Notes (
    id INT IDENTITY(1,1) PRIMARY KEY,
    userId INT NOT NULL,

    title NVARCHAR(100),
    subject NVARCHAR(100),
    source NVARCHAR(100),

    uploadDate DATETIME DEFAULT GETDATE(),

    fileType NVARCHAR(30),
    fileName NVARCHAR(255),
    filePath NVARCHAR(500),

    description NVARCHAR(1000),

    isPrivate BIT DEFAULT 0,
    status NVARCHAR(20) DEFAULT 'Pending',

    FOREIGN KEY(userId) REFERENCES Users(id)
);
GO

CREATE TABLE Resources (
    id INT IDENTITY(1,1) PRIMARY KEY,

    -- Optional link to an approved note
    noteId INT NULL,

    -- User who uploaded the resource
    uploadedBy INT NOT NULL,

    -- Resource information
    title NVARCHAR(100) NOT NULL,
    subject NVARCHAR(100),
    source NVARCHAR(100),
    description NVARCHAR(MAX),

    -- File information
    filePath NVARCHAR(500) NOT NULL,
    fileType NVARCHAR(30),

    -- Dates
    uploadDate DATETIME DEFAULT GETDATE(),

    -- Statistics
    downloads INT DEFAULT 0,

    -- Admin controls
    isActive BIT DEFAULT 1,
    status NVARCHAR(20) DEFAULT 'Pending',

    CONSTRAINT FK_Resources_Users
        FOREIGN KEY (uploadedBy)
        REFERENCES Users(id),

    CONSTRAINT FK_Resources_Notes
        FOREIGN KEY (noteId)
        REFERENCES Notes(id)
);
GO

CREATE TABLE Tasks (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    title NVARCHAR(100),
    description NVARCHAR(1000),
    status NVARCHAR(20) DEFAULT 'pending',
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(id)
);
GO

CREATE TABLE Questions (
    question_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,

    author_name NVARCHAR(100),
    subject NVARCHAR(100),
    question_text NVARCHAR(MAX),

    tags NVARCHAR(255),
    attachment_path NVARCHAR(500),

    reward_points INT DEFAULT 0,
    votes INT DEFAULT 0,
    views INT DEFAULT 0,

    created_at DATETIME DEFAULT GETDATE(),
    is_locked BIT DEFAULT 0,

    FOREIGN KEY(user_id) REFERENCES Users(id)
);
GO

CREATE TABLE QuestionVotes (
    id INT IDENTITY(1,1) PRIMARY KEY,

    question_id INT NOT NULL,
    user_id INT NOT NULL,

    voteDate DATETIME DEFAULT GETDATE(),

    CONSTRAINT FK_QuestionVotes_Question
        FOREIGN KEY (question_id)
        REFERENCES Questions(question_id),

    CONSTRAINT FK_QuestionVotes_User
        FOREIGN KEY (user_id)
        REFERENCES Users(id),

    CONSTRAINT UQ_Question_User
        UNIQUE(question_id, user_id)
);
GO

CREATE TABLE Answers (
    answer_id INT IDENTITY(1,1) PRIMARY KEY,
    question_id INT NOT NULL,
    user_id INT NOT NULL,

    author_name NVARCHAR(100),
    answer_text NVARCHAR(MAX),

    votes INT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY(question_id) REFERENCES Questions(question_id),
    FOREIGN KEY(user_id) REFERENCES Users(id)
);
GO

CREATE TABLE UploadedFiles (
    id VARCHAR(100) PRIMARY KEY,
    file_name NVARCHAR(255),
    file_path NVARCHAR(MAX),
    file_type NVARCHAR(50),
    uploaded_by INT,
    upload_date DATETIME DEFAULT GETDATE(),

    FOREIGN KEY(uploaded_by) REFERENCES Users(id)
);
GO

CREATE TABLE ActivityLogs (
    id INT IDENTITY(1,1) PRIMARY KEY,
    admin_id INT NOT NULL,
    admin_name NVARCHAR(100) NOT NULL,

    action NVARCHAR(255) NOT NULL,
    target_type NVARCHAR(100),
    target_name NVARCHAR(255),

    status NVARCHAR(50) DEFAULT 'SUCCESS',
    remarks NVARCHAR(MAX),

    created_at DATETIME DEFAULT GETDATE()
);
GO

CREATE TABLE Notifications (
    id INT IDENTITY(1,1) PRIMARY KEY,

    userId INT NOT NULL,

    title NVARCHAR(200) NOT NULL,

    message NVARCHAR(MAX) NOT NULL,

    type NVARCHAR(50) DEFAULT 'General',

    isRead BIT DEFAULT 0,

    created_at DATETIME DEFAULT GETDATE(),

    FOREIGN KEY (userId) REFERENCES Users(id)
);
GO

-- ============================================================================
-- PART 2: SCHEMA FIXES & BACKFILL
-- ============================================================================

-- Users.status column
IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'Users'
      AND COLUMN_NAME = 'status'
)
BEGIN
    ALTER TABLE dbo.Users
    ADD status NVARCHAR(20)
    CONSTRAINT DF_Users_status DEFAULT ('Active') WITH VALUES;
END
GO

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'Users'
      AND COLUMN_NAME = 'status'
)
BEGIN
    UPDATE dbo.Users
    SET status = 'Active'
    WHERE status IS NULL;
END
GO

-- Notes.status column
IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'Notes'
      AND COLUMN_NAME = 'status'
)
BEGIN
    ALTER TABLE dbo.Notes
    ADD status NVARCHAR(20)
    CONSTRAINT DF_Notes_status DEFAULT ('Pending') WITH VALUES;
END
GO

IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'dbo'
      AND TABLE_NAME = 'Notes'
      AND COLUMN_NAME = 'status'
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'dbo'
          AND TABLE_NAME = 'Notes'
          AND COLUMN_NAME = 'isPrivate'
    )
    BEGIN
        UPDATE dbo.Notes
        SET status =
            CASE
                WHEN ISNULL(isPrivate, 0) = 0 THEN 'Approved'
                ELSE 'Pending'
            END;
    END
    ELSE
    BEGIN
        UPDATE dbo.Notes
        SET status = 'Pending'
        WHERE status IS NULL;
    END
END
GO

-- ============================================================================
-- PART 3: ADMIN USER & NOTIFICATIONS
-- ============================================================================

UPDATE Users
SET
    name = 'Admin',
    email = 'admin@studybuddy.com',
    password = 'admin123',
    role = 'admin',
    status = 'Active'
WHERE email = 'admin@studybuddy.com';
GO

INSERT INTO Notifications
(userId, title, message, type)
VALUES
(
2,
'Welcome',
'Welcome to StudyBuddy!',
'System'
);
GO

-- ============================================================================
-- PART 4: DATABASE ROLES
-- ============================================================================

ALTER ROLE db_datareader ADD MEMBER studybuddy;
ALTER ROLE db_datawriter ADD MEMBER studybuddy;
ALTER ROLE db_owner ADD MEMBER studybuddy;
GO

-- ============================================================================
-- PART 5: ADMIN USER INSERT (IF NOT EXISTS)
-- ============================================================================

IF NOT EXISTS (
    SELECT 1
    FROM Users
    WHERE email='admin@studybuddy.com'
)
BEGIN
    INSERT INTO Users
    (
        name,
        email,
        password,
        role,
        status
    )
    VALUES
    (
        'Admin',
        'admin@studybuddy.com',
        'admin123',
        'ADMIN',
        'Active'
    );
END
GO

-- ============================================================================
-- PART 6: ADDITIONAL COLUMNS & CONSTRAINTS
-- ============================================================================

ALTER TABLE ActivityLogs
ADD CONSTRAINT FK_Activity_Admin
FOREIGN KEY(admin_id)
REFERENCES Users(id);
GO

ALTER TABLE Resources
ADD uploaderName NVARCHAR(100);
GO

ALTER TABLE Notes
ADD uploaderName NVARCHAR(100);
GO

-- ============================================================================
-- PART 7: LOOKUP TABLES (Departments, Semesters, Subjects)
-- ============================================================================

-- Departments Table
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Departments')
BEGIN
    CREATE TABLE Departments (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL UNIQUE,
        code NVARCHAR(10) NOT NULL UNIQUE,
        description NVARCHAR(500),
        isActive BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE()
    );
    PRINT 'Created Departments table';
END
ELSE
BEGIN
    PRINT 'Departments table already exists';
END
GO

-- Semesters Table
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Semesters')
BEGIN
    CREATE TABLE Semesters (
        id INT IDENTITY(1,1) PRIMARY KEY,
        departmentId INT NOT NULL,
        semesterNumber INT NOT NULL,
        name NVARCHAR(50) NOT NULL,
        description NVARCHAR(500),
        isActive BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (departmentId) REFERENCES Departments(id),
        UNIQUE (departmentId, semesterNumber)
    );
    PRINT 'Created Semesters table';
END
ELSE
BEGIN
    PRINT 'Semesters table already exists';
END
GO

-- Subjects Table
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Subjects')
BEGIN
    CREATE TABLE Subjects (
        id INT IDENTITY(1,1) PRIMARY KEY,
        semesterId INT NOT NULL,
        name NVARCHAR(200) NOT NULL,
        code NVARCHAR(20) NOT NULL,
        description NVARCHAR(500),
        credits INT DEFAULT 3,
        isActive BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (semesterId) REFERENCES Semesters(id),
        UNIQUE (semesterId, code)
    );
    PRINT 'Created Subjects table';
END
ELSE
BEGIN
    PRINT 'Subjects table already exists';
END
GO

-- ============================================================================
-- PART 8: POPULATE DEPARTMENTS (Idempotent)
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM Departments WHERE code = 'BCA')
BEGIN
    INSERT INTO Departments (name, code, description, isActive)
    VALUES (
        'Bachelor of Computer Application',
        'BCA',
        'Bachelor of Computer Application (BCA) program at Pokhara University - 4 years, 8 semesters, 126 credit hours',
        1
    );
    PRINT 'Inserted BCA Department';
END
ELSE
BEGIN
    PRINT 'BCA Department already exists - skipping';
END
GO

IF NOT EXISTS (SELECT 1 FROM Departments WHERE code = 'BBA')
BEGIN
    INSERT INTO Departments (name, code, description, isActive)
    VALUES (
        'Bachelor of Business Administration',
        'BBA',
        'Bachelor of Business Administration (BBA) program at Pokhara University - 4 years, 8 semesters, 120 credit hours',
        1
    );
    PRINT 'Inserted BBA Department';
END
ELSE
BEGIN
    PRINT 'BBA Department already exists - skipping';
END
GO

-- ============================================================================
-- PART 9: POPULATE SEMESTERS (Idempotent) - Using inline SELECTs
-- ============================================================================

-- BCA Semesters (1-8)
IF NOT EXISTS (SELECT 1 FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1)
BEGIN
    INSERT INTO Semesters (departmentId, semesterNumber, name, description, isActive)
    VALUES 
        ((SELECT id FROM Departments WHERE code = 'BCA'), 1, 'First Semester', 'BCA First Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 2, 'Second Semester', 'BCA Second Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 3, 'Third Semester', 'BCA Third Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 4, 'Fourth Semester', 'BCA Fourth Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 5, 'Fifth Semester', 'BCA Fifth Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 6, 'Sixth Semester', 'BCA Sixth Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 7, 'Seventh Semester', 'BCA Seventh Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BCA'), 8, 'Eighth Semester', 'BCA Eighth Semester - Pokhara University', 1);
    PRINT 'Inserted BCA Semesters 1-8';
END
ELSE
BEGIN
    PRINT 'BCA Semesters already exist - skipping';
END
GO

-- BBA Semesters (1-8)
IF NOT EXISTS (SELECT 1 FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 1)
BEGIN
    INSERT INTO Semesters (departmentId, semesterNumber, name, description, isActive)
    VALUES 
        ((SELECT id FROM Departments WHERE code = 'BBA'), 1, 'First Semester', 'BBA First Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 2, 'Second Semester', 'BBA Second Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 3, 'Third Semester', 'BBA Third Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 4, 'Fourth Semester', 'BBA Fourth Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 5, 'Fifth Semester', 'BBA Fifth Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 6, 'Sixth Semester', 'BBA Sixth Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 7, 'Seventh Semester', 'BBA Seventh Semester - Pokhara University', 1),
        ((SELECT id FROM Departments WHERE code = 'BBA'), 8, 'Eighth Semester', 'BBA Eighth Semester - Pokhara University', 1);
    PRINT 'Inserted BBA Semesters 1-8';
END
ELSE
BEGIN
    PRINT 'BBA Semesters already exist - skipping';
END
GO

-- ============================================================================
-- PART 10: BCA SUBJECTS - OFFICIAL POKHARA UNIVERSITY CURRICULUM
-- ============================================================================

PRINT '';
PRINT '--- Updating BCA Subjects (Official PU Curriculum) ---';

-- BCA SEMESTER 1 (Credits: 13)
PRINT '  Processing BCA Semester 1...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1), 'English for IT Professionals', 'ENG 121', 'English language and communication skills for IT professionals', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1), 'Mathematics I', 'MTH 131', 'Calculus, analytical geometry, and mathematical foundations for computing', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1), 'Digital Logic System', 'ELX 111', 'Boolean algebra, logic gates, digital circuits, and computer organization basics', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1), 'Computer Fundamentals and Applications', 'CMP 116', 'Introduction to computers, hardware, software, and application packages', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1), 'Programming Logic and Techniques', 'CMP 117', 'Problem solving, algorithms, flowcharts, and programming fundamentals', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 1), 'Computer Application Workshop', 'CMP 111', 'Hands-on practical workshop for computer applications and tools', 1)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 2 (Credits: 15)
PRINT '  Processing BCA Semester 2...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 2), 'Business and Technical Communication', 'ENG 122', 'Business writing, technical documentation, and professional communication', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 2), 'Mathematics II', 'MTH 132', 'Linear algebra, discrete mathematics, and advanced mathematical concepts', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 2), 'Financial Accounting', 'ACC 131', 'Principles of financial accounting, bookkeeping, and financial statements', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 2), 'Programming in C', 'CMP 118', 'Structured programming using C language, data types, control structures, and functions', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 2), 'Microprocessor and Computer Architecture', 'ELX 112', 'Microprocessor architecture, instruction set, assembly language, and computer organization', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 2), 'Project I', 'PRJ 151', 'First project work - basic programming and system development project', 1)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 3 (Credits: 15)
PRINT '  Processing BCA Semester 3...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 3), 'Object Oriented Programming in Java', 'CMP 215', 'OOP concepts, Java programming, classes, objects, inheritance, and polymorphism', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 3), 'Data Structure and Algorithms', 'CMP 227', 'Arrays, linked lists, stacks, queues, trees, graphs, and algorithm analysis', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 3), 'System Analysis and Project Management', 'CMP 221', 'SDLC, requirement analysis, UML, project planning, and management techniques', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 3), 'Web Technologies I', 'CMP 380', 'HTML, CSS, JavaScript, and client-side web development fundamentals', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 3), 'Operating System', 'CMP 230', 'Process management, memory management, file systems, and OS concepts', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 4 (Credits: 17)
PRINT '  Processing BCA Semester 4...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 4), 'Software Engineering', 'CMP 323', 'Software development lifecycle, design patterns, testing, and quality assurance', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 4), 'Database Management System', 'CMP 226', 'Relational database concepts, SQL, normalization, and transaction management', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 4), 'Computer Graphics and Multimedia Technology', 'CMP 242', '2D/3D graphics, rendering, animation, and multimedia authoring tools', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 4), 'Probability and Statistics', 'MTH 320', 'Probability theory, statistical distributions, hypothesis testing, and data analysis', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 4), 'Web Technologies II', 'CMP 402', 'Server-side programming, PHP, database connectivity, and web application development', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 4), 'Project II', 'PRJ 251', 'Second project work - database-driven web application development', 2)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 5 (Credits: 15)
PRINT '  Processing BCA Semester 5...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 5), 'Numerical Methods', 'MTH 230', 'Numerical analysis, interpolation, integration, differential equations, and error analysis', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 5), 'DotNet Technology', 'CMP 317', 'ASP.NET, C# programming, web forms, and Microsoft .NET framework development', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 5), 'Data Communication and Computer Network', 'CMP 336', 'Network protocols, TCP/IP, OSI model, routing, and network architecture', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 5), 'Research Methodology', 'ELE 322', 'Research design, data collection methods, statistical analysis, and report writing', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 5), 'Mathematical Foundation of Computer Science', 'MTH 330', 'Discrete structures, automata theory, formal languages, and computability', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 6 (Credits: 14)
PRINT '  Processing BCA Semester 6...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 6), 'Data Science and Analytics', 'CMP 316', 'Data mining, machine learning basics, data visualization, and analytical tools', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 6), 'Management Information System', 'CMP 314', 'MIS concepts, decision support systems, ERP, and business intelligence', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 6), 'Simulation and Modeling', 'CMP 350', 'System simulation, modeling techniques, Monte Carlo methods, and simulation software', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 6), 'Organization Management', 'MGT 322', 'Organizational behavior, leadership, team management, and HR fundamentals', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 6), 'Elective I', 'ELE 601', 'Specialized elective course chosen from available options (Computer/Management)', 2),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 6), 'Project III', 'PRJ 351', 'Third project work - advanced application development with research component', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 7 (Credits: 12)
PRINT '  Processing BCA Semester 7...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 7), 'Cyber Law and Professional Ethics', 'CMP 401', 'IT laws, cybercrime, intellectual property, privacy, and professional ethics', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 7), 'Mobile Application Development Technology', 'CMP 404', 'Android and iOS development, mobile UI/UX, and cross-platform frameworks', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 7), 'Applied Economics', 'ECO 311', 'Economic principles, market analysis, and business economics applications', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 7), 'Internship', 'INT 461', 'Industrial internship - real-world software development experience in IT organizations', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 7), 'Elective II', 'ELE 701', 'Advanced specialized elective course chosen from available options', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BCA SEMESTER 8 (Credits: 14)
PRINT '  Processing BCA Semester 8...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 8), 'Cloud Computing', 'CMP 415', 'Cloud architecture, AWS/Azure services, virtualization, and distributed systems', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 8), 'Digital Economy', 'CMP 416', 'Digital transformation, e-commerce, fintech, and digital business models', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 8), 'Elective III', 'ELE 801', 'Advanced specialized elective course for final semester specialization', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BCA') AND semesterNumber = 8), 'Project IV', 'PRJ 451', 'Final year project - comprehensive software development with research and documentation', 5)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- ============================================================================
-- PART 11: BBA SUBJECTS - OFFICIAL POKHARA UNIVERSITY CURRICULUM
-- ============================================================================

PRINT '';
PRINT '--- Updating BBA Subjects (Official PU Curriculum) ---';

-- BBA SEMESTER 1 (Credits: 15)
PRINT '  Processing BBA Semester 1...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 1), 'English I', 'ENG 101', 'English language proficiency, grammar, composition, and communication skills', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 1), 'Business Mathematics I', 'MTH 101', 'Algebra, calculus, and mathematical applications in business contexts', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 1), 'Computer and IT Applications', 'MIS 101', 'Information technology fundamentals, office applications, and digital literacy', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 1), 'Financial Accounting I', 'ACC 121', 'Principles of accounting, journal entries, ledger, and financial statements', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 1), 'Principles of Management', 'MGT 111', 'Management theories, planning, organizing, leading, and controlling functions', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 2 (Credits: 15)
PRINT '  Processing BBA Semester 2...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 2), 'English II', 'ENG 102', 'Advanced English communication, business writing, and presentation skills', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 2), 'Business Mathematics II', 'MTH 102', 'Advanced calculus, linear algebra, and quantitative business methods', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 2), 'Financial Accounting II', 'ACC 122', 'Advanced accounting concepts, partnership, company accounts, and analysis', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 2), 'General Psychology', 'PSY 101', 'Psychological principles, human behavior, cognition, and organizational psychology', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 2), 'Introductory Microeconomics', 'ECO 101', 'Microeconomic theory, demand-supply, consumer behavior, and market structures', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 3 (Credits: 15)
PRINT '  Processing BBA Semester 3...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 3), 'Business Communication I', 'ENG 201', 'Business correspondence, report writing, and professional communication', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 3), 'Business Statistics', 'STT 101', 'Statistical methods, probability, data analysis, and business decision making', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 3), 'Essentials of Finance', 'FIN 131', 'Financial management basics, time value of money, and investment principles', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 3), 'Fundamentals of Sociology', 'SOC 101', 'Sociological concepts, social structures, culture, and organizational sociology', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 3), 'Introductory Macroeconomics', 'ECO 201', 'Macroeconomic theory, national income, monetary policy, and fiscal policy', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 4 (Credits: 15)
PRINT '  Processing BBA Semester 4...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 4), 'Business Communication II', 'ENG 202', 'Advanced business communication, negotiation, and cross-cultural communication', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 4), 'Data Analysis and Modeling', 'STT 201', 'Advanced statistical analysis, regression, forecasting, and business modeling', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 4), 'Fundamentals of Organizational Behaviour', 'MGT 211', 'Individual behavior, motivation, leadership, and organizational culture', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 4), 'Principles of Marketing', 'MKT 241', 'Marketing concepts, consumer behavior, product management, and marketing strategies', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 4), 'Financial Management', 'FIN 231', 'Corporate finance, capital budgeting, risk analysis, and financial planning', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 5 (Credits: 15)
PRINT '  Processing BBA Semester 5...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 5), 'Basics of Managerial Accounting', 'ACC 221', 'Cost accounting, budgeting, variance analysis, and managerial decision making', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 5), 'Business Research Methods', 'RCH 311', 'Research design, data collection, statistical analysis, and report preparation', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 5), 'Management of Human Resources', 'MGT 314', 'HR planning, recruitment, training, performance management, and labor laws', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 5), 'Fundamentals of Operations Management', 'MGT 311', 'Production planning, supply chain, quality control, and operations strategy', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 5), 'Concentration I', 'CON 501', 'Specialized concentration course I (Accounting/Marketing/Finance/HR/SBE)', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 6 (Credits: 15)
PRINT '  Processing BBA Semester 6...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 6), 'Introduction to Management Information Systems', 'MIS 201', 'MIS concepts, database management, ERP systems, and IT in business', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 6), 'Legal Aspects of Business and Technology', 'LAW 291', 'Business law, contract law, company law, and technology legal frameworks', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 6), 'Business and Society', 'MGT 212', 'Corporate social responsibility, ethics, sustainability, and stakeholder management', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 6), 'Project Work', 'PRJ 491', 'Independent research project with fieldwork, data analysis, and report writing', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 6), 'Concentration II', 'CON 601', 'Specialized concentration course II (Accounting/Marketing/Finance/HR/SBE)', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 7 (Credits: 15)
PRINT '  Processing BBA Semester 7...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 7), 'Business Environment in Nepal', 'MGT 411', 'Nepalese business environment, economic policies, and regulatory framework', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 7), 'Fundamentals of Entrepreneurship', 'MGT 312', 'Entrepreneurship concepts, business planning, startup management, and innovation', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 7), 'Internship', 'INT 391', 'Industrial internship - hands-on experience in business organizations', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 7), 'Elective I', 'ELE 701', 'Non-business elective course (Society/Politics/Econometrics/Environment/Media)', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 7), 'Concentration III', 'CON 701', 'Specialized concentration course III (Accounting/Marketing/Finance/HR/SBE)', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- BBA SEMESTER 8 (Credits: 15)
PRINT '  Processing BBA Semester 8...';
MERGE INTO Subjects AS target
USING (VALUES
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 8), 'Strategic Management', 'MGT 412', 'Strategic planning, competitive analysis, corporate strategy, and implementation', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 8), 'Introduction to International Business', 'MGT 313', 'Global trade, international marketing, cross-border management, and WTO', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 8), 'Essentials of e-Business', 'MIS 301', 'E-commerce, digital marketing, online business models, and payment systems', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 8), 'Elective II', 'ELE 801', 'Non-business elective course II (Society/Politics/Econometrics/Environment/Media)', 3),
    ((SELECT id FROM Semesters WHERE departmentId = (SELECT id FROM Departments WHERE code = 'BBA') AND semesterNumber = 8), 'Concentration IV', 'CON 801', 'Specialized concentration course IV (Accounting/Marketing/Finance/HR/SBE)', 3)
) AS source (semesterId, name, code, description, credits)
ON target.semesterId = source.semesterId AND target.code = source.code
WHEN MATCHED THEN
    UPDATE SET 
        target.name = source.name,
        target.description = source.description,
        target.credits = source.credits,
        target.isActive = 1
WHEN NOT MATCHED THEN
    INSERT (semesterId, name, code, description, credits, isActive)
    VALUES (source.semesterId, source.name, source.code, source.description, source.credits, 1);
GO

-- ============================================================================
-- PART 12: CLEAN UP OLD/INVALID SUBJECTS
-- ============================================================================

PRINT '';
PRINT '--- Deactivating old non-PU curriculum subjects ---';

UPDATE Subjects
SET isActive = 0
WHERE isActive = 1
  AND code NOT IN (
    -- BCA Official Codes
    'ENG 121', 'MTH 131', 'ELX 111', 'CMP 116', 'CMP 117', 'CMP 111',
    'ENG 122', 'MTH 132', 'ACC 131', 'CMP 118', 'ELX 112', 'PRJ 151',
    'CMP 215', 'CMP 227', 'CMP 221', 'CMP 380', 'CMP 230',
    'CMP 323', 'CMP 226', 'CMP 242', 'MTH 320', 'CMP 402', 'PRJ 251',
    'MTH 230', 'CMP 317', 'CMP 336', 'ELE 322', 'MTH 330',
    'CMP 316', 'CMP 314', 'CMP 350', 'MGT 322', 'ELE 601', 'PRJ 351',
    'CMP 401', 'CMP 404', 'ECO 311', 'INT 461', 'ELE 701',
    'CMP 415', 'CMP 416', 'ELE 801', 'PRJ 451',
    -- BBA Official Codes
    'ENG 101', 'MTH 101', 'MIS 101', 'ACC 121', 'MGT 111',
    'ENG 102', 'MTH 102', 'ACC 122', 'PSY 101', 'ECO 101',
    'ENG 201', 'STT 101', 'FIN 131', 'SOC 101', 'ECO 201',
    'ENG 202', 'STT 201', 'MGT 211', 'MKT 241', 'FIN 231',
    'ACC 221', 'RCH 311', 'MGT 314', 'MGT 311', 'CON 501',
    'MIS 201', 'LAW 291', 'MGT 212', 'PRJ 491', 'CON 601',
    'MGT 411', 'MGT 312', 'INT 391', 'ELE 701', 'CON 701',
    'MGT 412', 'MGT 313', 'MIS 301', 'ELE 801', 'CON 801'
  );

DECLARE @DeactivatedCount INT = @@ROWCOUNT;
PRINT '  Deactivated ' + CAST(@DeactivatedCount AS VARCHAR) + ' old subjects (isActive = 0)';
GO

-- ============================================================================
-- PART 13: FOREIGN KEY LINKS TO SUBJECTS
-- ============================================================================

-- Add subjectId to Notes table
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Notes' AND COLUMN_NAME = 'subjectId'
)
BEGIN
    ALTER TABLE Notes ADD subjectId INT NULL;
    ALTER TABLE Notes ADD CONSTRAINT FK_Notes_Subject 
        FOREIGN KEY (subjectId) REFERENCES Subjects(id);
    PRINT 'Added subjectId to Notes table';
END
GO

-- Add subjectId to Resources table
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Resources' AND COLUMN_NAME = 'subjectId'
)
BEGIN
    ALTER TABLE Resources ADD subjectId INT NULL;
    ALTER TABLE Resources ADD CONSTRAINT FK_Resources_Subject 
        FOREIGN KEY (subjectId) REFERENCES Subjects(id);
    PRINT 'Added subjectId to Resources table';
END
GO

-- Add subjectId to Questions table
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Questions' AND COLUMN_NAME = 'subjectId'
)
BEGIN
    ALTER TABLE Questions ADD subjectId INT NULL;
    ALTER TABLE Questions ADD CONSTRAINT FK_Questions_Subject 
        FOREIGN KEY (subjectId) REFERENCES Subjects(id);
    PRINT 'Added subjectId to Questions table';
END
GO

-- ============================================================================
-- PART 14: HELPER VIEWS
-- ============================================================================

IF OBJECT_ID('vw_SubjectsDetail', 'V') IS NOT NULL
    DROP VIEW vw_SubjectsDetail;
GO

CREATE VIEW vw_SubjectsDetail AS
SELECT 
    s.id AS subjectId,
    s.name AS subjectName,
    s.code AS subjectCode,
    s.credits,
    sem.id AS semesterId,
    sem.name AS semesterName,
    sem.semesterNumber,
    d.id AS departmentId,
    d.name AS departmentName,
    d.code AS departmentCode
FROM Subjects s
INNER JOIN Semesters sem ON s.semesterId = sem.id
INNER JOIN Departments d ON sem.departmentId = d.id
WHERE s.isActive = 1 AND sem.isActive = 1 AND d.isActive = 1;
GO

PRINT 'Created vw_SubjectsDetail view';
GO


-- V001: Add UserActivities table and update existing tables for real-time stats

-- 1. Create UserActivities table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'UserActivities')
BEGIN
    CREATE TABLE UserActivities (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        user_full_name NVARCHAR(255),
        action NVARCHAR(50) NOT NULL,
        target_type NVARCHAR(50) NOT NULL,
        target_name NVARCHAR(255),
        created_at DATETIME DEFAULT GETDATE()
    );
END

-- 2. Add status column to Resources table if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Resources') AND name = 'status')
BEGIN
    ALTER TABLE Resources ADD status NVARCHAR(50) DEFAULT 'Pending';
END

-- 3. Add lastLogin column to Users table if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'lastLogin')
BEGIN
    ALTER TABLE Users ADD lastLogin DATETIME;
END

-- 4. Add indexes for better performance
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_user_activities_user_id' AND object_id = OBJECT_ID('UserActivities'))
BEGIN
    CREATE INDEX idx_user_activities_user_id ON UserActivities(user_id);
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_notes_status' AND object_id = OBJECT_ID('Notes'))
BEGIN
    CREATE INDEX idx_notes_status ON Notes(status);
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_resources_status' AND object_id = OBJECT_ID('Resources'))
BEGIN
    CREATE INDEX idx_resources_status ON Resources(status);
END

-- V004__create_settings_table.sql

-- Create Settings table if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Settings]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Settings] (
        [id] INT IDENTITY(1,1) PRIMARY KEY,
        [setting_key] NVARCHAR(100) UNIQUE NOT NULL,
        [setting_value] NVARCHAR(MAX),
        [description] NVARCHAR(255),
        [updated_by] INT NULL,
        [updated_at] DATETIME2 DEFAULT SYSUTCDATETIME(),
        
        CONSTRAINT FK_Settings_Users FOREIGN KEY ([updated_by]) REFERENCES [dbo].[Users]([id])
    );
    
    -- Add index for setting_key for faster lookups
    CREATE INDEX IX_Settings_setting_key ON [dbo].[Settings]([setting_key]);
END

-- Seed default settings if they don't exist
IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'maintenance_mode')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('maintenance_mode', 'false', 'Whether the application is in maintenance mode');

IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'allow_registration')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('allow_registration', 'true', 'Whether new user registration is allowed');

IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'default_department')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('default_department', 'BCA', 'Default department for new users');

IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'email_verification')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('email_verification', 'true', 'Whether email verification is required');

IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'allow_resource_upload')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('allow_resource_upload', 'true', 'Whether users can upload resources');

IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'allow_note_upload')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('allow_note_upload', 'true', 'Whether users can upload notes');

IF NOT EXISTS (SELECT * FROM [dbo].[Settings] WHERE [setting_key] = 'allow_question_posting')
    INSERT INTO [dbo].[Settings] ([setting_key], [setting_value], [description]) 
    VALUES ('allow_question_posting', 'true', 'Whether users can post questions');


-- ============================================================================
-- PART 15: VERIFICATION QUERIESs
-- ============================================================================

PRINT '';
PRINT '=================================================================';
PRINT 'DATABASE SETUP COMPLETED SUCCESSFULLY';
PRINT '=================================================================';
PRINT '';

-- Department Summary
SELECT 
    'Departments' AS Entity,
    COUNT(*) AS TotalCount,
    SUM(CASE WHEN isActive = 1 THEN 1 ELSE 0 END) AS ActiveCount
FROM Departments;

-- Semester Summary by Department
SELECT 
    d.code AS Department,
    COUNT(s.id) AS TotalSemesters,
    SUM(CASE WHEN s.isActive = 1 THEN 1 ELSE 0 END) AS ActiveSemesters
FROM Departments d
LEFT JOIN Semesters s ON d.id = s.departmentId
GROUP BY d.code, d.name
ORDER BY d.code;

-- Subject Summary by Department and Semester
SELECT 
    d.code AS Department,
    sem.semesterNumber AS Semester,
    COUNT(sub.id) AS TotalSubjects,
    SUM(CASE WHEN sub.isActive = 1 THEN 1 ELSE 0 END) AS ActiveSubjects,
    SUM(sub.credits) AS TotalCredits
FROM Departments d
INNER JOIN Semesters sem ON d.id = sem.departmentId
LEFT JOIN Subjects sub ON sem.id = sub.semesterId
GROUP BY d.code, sem.semesterNumber
ORDER BY d.code, sem.semesterNumber;

-- Total Credits by Department
SELECT 
    d.code AS Department,
    SUM(sub.credits) AS TotalCredits,
    COUNT(sub.id) AS TotalSubjects
FROM Departments d
INNER JOIN Semesters sem ON d.id = sem.departmentId
INNER JOIN Subjects sub ON sem.id = sub.semesterId
WHERE sub.isActive = 1
GROUP BY d.code;

-- Dashboard Summary
SELECT
    (SELECT COUNT(*) FROM Users WHERE status<>'Deleted') AS TotalUsers,
    (SELECT COUNT(*) FROM Notes WHERE status<>'Deleted') AS TotalNotes,
    (SELECT COUNT(*) FROM Resources WHERE isActive=1) AS TotalResources,
    (SELECT COUNT(*) FROM Questions) AS TotalQuestions,
    (SELECT COUNT(*) FROM Answers) AS TotalAnswers,
    (SELECT COUNT(*) FROM Tasks) AS TotalTasks,
    (SELECT COUNT(*) FROM Subjects WHERE isActive=1) AS ActiveSubjects,
    (SELECT COUNT(*) FROM Departments WHERE isActive=1) AS ActiveDepartments,
    (SELECT COUNT(*) FROM Semesters WHERE isActive=1) AS ActiveSemesters;

PRINT '';
PRINT 'End of script.';
GO
--DROP TABLE Answers;
--DROP TABLE Questions;
-- DROP TABLE Resources;
-- DROP TABLE Notes;
--DROP TABLE Tasks;
-- DROP TABLE Users;


SELECT * FROM dbo.Users;
SELECT * FROM Users;
SELECT * FROM Questions;
SELECT * FROM Answers;
SELECT * FROM Notes;
SELECT * FROM Resources;
SELECT * FROM Tasks;
SELECT * FROM UploadedFiles;
SELECT * FROM Notifications
SELECT status FROM Users;
SELECT status FROM Notes;
SELECT * FROM QuestionVotes;
GO