import React from "react";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";

import ResearcherTopBar from "../../components/researcher/ResearcherTopBar";
import UserTopBar from "../../components/user/UserTopBar";

export default function TopBar() {
  const { mode } = useModeStore();

  // Use mode to determine which TopBar to show
  // mode can be "researcher", "USER", or null
  // If mode is null (not set), default to USER mode
  return mode === "researcher" ? <ResearcherTopBar /> : <UserTopBar />;
}
