import React from "react";
import { useAuthStore } from "../../stores/authStore";
import { useModeStore } from "../../stores/modeStore";

import AdminTopBar from "../../components/admin/AdminTopBar";
import UserTopBar from "../../components/user/UserTopBar";

export default function TopBar() {
  const { role } = useAuthStore();
  const { mode } = useModeStore();

  const effectiveMode = role === "ADMIN" ? "ADMIN" : "USER";

  return effectiveMode === "ADMIN" ? <AdminTopBar /> : <UserTopBar />;
}
