import React from "react";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";

import AdminTopBar from "../../components/admin/AdminTopBar";
import UserTopBar from "../../components/user/UserTopBar";

export default function TopBar() {
  const { mode } = useModeStore();

  // Use mode to determine which TopBar to show
  // mode can be "ADMIN", "USER", or null
  // If mode is null (not set), default to USER mode
  return mode === "ADMIN" ? <AdminTopBar /> : <UserTopBar />;
}
