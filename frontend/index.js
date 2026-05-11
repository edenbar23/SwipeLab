import { registerRootComponent } from "expo";
import { Platform } from "react-native";
import App from "./App";

// Fix for mobile web browsers: Ensure the root viewport matches the visible screen
// dynamically (100dvh) to prevent the address bar from cutting off the bottom navigation.
if (Platform.OS === "web") {
  const style = document.createElement("style");
  style.innerHTML = `
    html, body, #root {
      height: 100dvh !important;
      overflow: hidden !important;
    }
  `;
  document.head.appendChild(style);
}

// This ensures the environment loads your App component first.
registerRootComponent(App);
