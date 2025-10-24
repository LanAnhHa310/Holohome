### HoloHome - AR Furniture App
## What it does
HoloHome helps you see how furniture looks in your room before you buy it. Browse furniture, use your phone camera to place items in your space, and save room designs you like.

## Key Features:
- Browse furniture by category (tables, chairs, desks)
- Filter by price, color, and material
- Place furniture using AR camera (coming soon)
- Save your favorite room layouts
- Create account and manage settings

## Why use it?
- Save money
- Preview complete rooms, not just one item
- No design skills needed
- Make sure items match and fit

## Who is it for?
- Students furnishing dorm rooms
- New homeowners on a budget
- Anyone who feels overwhelmed shopping for furniture online

# Getting Started
## What you need
- Android Studio
- Android phone or emulator (Android 7.0 or newer)

1. Clone the repository: git clone https://github.com/GU-App-Development2025/final-project-final-project-team-2.git

2. Open the project in Android Studio

3. Sync Gradle files and resolve any dependencies

4. Build and run the application on your emulator or physical device

## First Time Setup
1. Launch the app, you'll be directed to the Create Account screen
2. Enter your full name, email, and password (min 8 characters with letters & numbers)
3. Agree to terms and create your account
4. You'll be automatically signed in and directed to the main browse screen

## Current Features

## Implemented
- **User Authentication System**
  - Create account with validation (email format, password strength)
  - Sign in with remember me functionality
  - Account settings (change password, profile picture)
  
- **Furniture Browse Interface**
  - Category tabs (Tables, Chairs, Desks)
  - Grid layout display with 3 columns
  - Search functionality by name or tags
  - Advanced filtering system:
    - Price range slider
    - Color selection (dynamic chips)
    - Material selection (dynamic chips)
    - Sort options (price ascending/descending, name A-Z)
  
- **Navigation System**
  - Settings menu with search functionality
  - Account settings access
  - Saved furniture and layouts screens (UI only)
  - Hamburger menu integration

## In Progress
- AR camera integration for furniture placement
- Multi-item AR scene management
- Save/load custom room layouts
- Product detail expansion
- Store link integration

# Project Structure
```
app/
├── manifests/
│   └── AndroidManifest.xml
├── kotlin/
│   └── com.zybooks.appmobilefinalproject/
│       ├── AccountSettingsActivity.kt
│       ├── CreateAccountActivity.kt
│       ├── FurnitureAdapter.kt
│       ├── GridSpacingItemDecoration.kt
│       ├── MainActivity.kt
│       ├── SettingsActivity.kt
│       └── SignInActivity.kt
└── res/
    └── layout/
        ├── account_settings.xml
        ├── activity_main.xml
        ├── create_account_activity.xml
        ├── fav_furn.xml
        ├── filter_sheet.xml
        ├── item_furniture.xml
        ├── item_saved_panel.xml
        ├── saved_lay.xml
        ├── settings_view.xml
        └── sign_in.xml

```
## Contributing
- We use a branch-based workflow with individual developer branches. Direct commits to main are allowed only for safe, quick fixes (e.g., README updates, minor typos).
# Active Development Branches
- Steph: usanomi - UI/UX, menus, settings, saved furniture screens
- Anh: LanAnhHa310 - Authentication, main screens, filters, saved layout screens

## Credits
This application was developed by the Gonzaga University Mobile Development Team: "Mango Matcha"
# Team Members
1. Thi Lan Anh Ha (LanAnhHa310)
2. Steph Borla (Usanomi)

## Course Information
Course: CPSC 333 - Mobile Application Development
Institution: Gonzaga University
Semester: Fall 2025
Instructor: Daniel Olivares

## Technologies Used
- Kotlin
- Android SDK
- Material Design Components
- ARCore (planned)
- SharedPreferences
