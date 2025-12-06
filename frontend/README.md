# Welcome to your Expo app 👋

This is an [Expo](https://expo.dev) project created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).

## Get started

1. Install dependencies

   ```bash
   npm install
   ```

2. Start the app

   ```bash
   npx expo start
   ```

In the output, you'll find options to open the app in a

- [development build](https://docs.expo.dev/develop/development-builds/introduction/)
- [Android emulator](https://docs.expo.dev/workflow/android-studio-emulator/)
- [iOS simulator](https://docs.expo.dev/workflow/ios-simulator/)
- [Expo Go](https://expo.dev/go), a limited sandbox for trying out app development with Expo

You can start developing by editing the files inside the **app** directory. This project uses [file-based routing](https://docs.expo.dev/router/introduction).

## Get a fresh project

When you're ready, run:

```bash
npm run reset-project
```

## Directory Structure
```bash
frontend/
│
├── app/
│   ├── navigation/
│   │   ├── RootNavigator.tsx     # selects User/Admin flows
│   │   ├── UserNavigator.tsx     # normal user screens
│   │   └── AdminNavigator.tsx    # admin dashboard screens
│   │
│   ├── screens/
│   │   ├── user/                 # screens for User mode
│   │   │   ├── SwipeScreen.tsx
│   │   │   ├── LeaderboardScreen.tsx
│   │   │   └── TasksScreen.tsx
│   │   │
│   │   ├── admin/                # screens ONLY admin sees
│   │   │   ├── AdminDashboard.tsx
│   │   │   ├── UserAnalyticsScreen.tsx
│   │   │   └── DatasetManagementScreen.tsx
│   │   │
│   │   └── shared/               # shared screens (Profile, Settings, Login)
│   │       ├── LoginScreen.tsx
│   │       ├── ProfileScreen.tsx
│   │       └── SettingsScreen.tsx
│   │
│   ├── components/               # UI components reused across modes
│   │   ├── Swiper/
│   │   └── Buttons/
│   │
│   ├── stores/                   # global state (Zustand)
│   │   ├── authStore.ts          # stores token & role
│   │   └── modeStore.ts          # "user" | "admin" (UI mode)
│   │
│   ├── services/                 # API requests
│   │   ├── authService.ts
│   │   ├── imageService.ts
│   │   └── adminService.ts
│   │
│   └── utils/                    # constants, helpers
│
└── assets/

```
