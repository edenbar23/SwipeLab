import * as Google from "expo-auth-session/providers/google";
import * as WebBrowser from "expo-web-browser";
import React, { useEffect, useState } from "react";
import { ActivityIndicator, Image, Platform, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from "../../api/apiFetch";
import { preloadAfterLogin } from "../../api/queries";
import RegisterForm from "../../components/RegisterForm";
import { useAuthStore } from "../../stores/authStore";
import useResponsive from "../../hooks/useResponsive";



WebBrowser.maybeCompleteAuthSession();

export default function LoginScreen() {
  const setAuth = useAuthStore((s) => s.setAuth);
  const setExternalAuth = useAuthStore((s) => s.setExternalAuth);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showRegister, setShowRegister] = useState(false);

  const [request, response, promptAsync] = Google.useAuthRequest({
    clientId: "<YOUR_GOOGLE_CLIENT_ID>",
  });

  useEffect(() => {
    if (response?.type === "success") {
      const { authentication } = response;
      const token = "mock-jwt-token";
      const role = "ADMIN";

      // Needs async wrapper
      (async () => {
        setLoading(true);
        await setAuth(token, role);
        // Preload cache
        await preloadAfterLogin(role);
        setLoading(false);
      })();
    }
  }, [response]);

  const handleLogin = async () => {
    setLoading(true);
    setError("");

    try {
      const res = await apiFetch(API_ENDPOINTS.AUTH.LOGIN, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          password,
        }),
      });

      if (!res.ok) {
        try {
          const errorData = await res.json();
          setError(errorData.message || "Invalid username or password");
        } catch {
          setError(`Login failed: ${res.status}`);
        }
        return;
      }

      const data = await res.json();
      const role = data.user.role;

      // data comes from auth.mock.ts
      await setAuth(data.accessToken, role, data.refreshToken);

      // Blocking prefetch before navigate
      await preloadAfterLogin(role);
    } catch (e) {
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleExternalLogin = async () => {
    setLoading(true);
    setError("");

    try {
      // 1. Call Stardbi directly
      const stardbiRes = await fetch(API_ENDPOINTS.STARDBI.LOGIN, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          password,
        }),
      });

      if (!stardbiRes.ok) {
        setError("Invalid username or password for Researcher login");
        return;
      }

      const stardbiData = await stardbiRes.json();

      // 2. Map through backend – send the full Stardbi response so the backend
      //    can validate the token AND auto-provision the user if first login.
      const backendRes = await apiFetch("/api/v1/auth/external/stardbi/loginExternal", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          access: stardbiData.access,
          refresh: stardbiData.refresh,
          lifetime: stardbiData.lifetime,
          id: stardbiData.id,
          username: stardbiData.username || username,
          first_name: stardbiData.first_name,
          last_name: stardbiData.last_name,
          email: stardbiData.email,
        }),
      });


      if (!backendRes.ok) {
        setError("Backend failed to validate Researcher credentials");
        return;
      }

      // 3. Store authentication context locally
      await setExternalAuth(stardbiData.access, stardbiData.refresh, stardbiData.lifetime || 0, stardbiData.username || username);

      // Blocking prefetch before navigate
      // Role can be assumed RESEARCHER or ADMIN for external
      await preloadAfterLogin("ADMIN");
    } catch (e) {
      console.error("[LoginScreen] Network/Fetch Exception Caught:", e);
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };


  const { isDesktop } = useResponsive();

  return (
    <View style={[styles.screenContainer, isDesktop && styles.webScreenContainer]}>
      {/* LOGIN CARD */}
      <View style={[styles.container, isDesktop && styles.webCard, showRegister && { opacity: 0.75 }]}>
        <Image source={require("../../../assets/images/icon.png")} style={styles.logo} />
        <Text style={styles.title}>Welcome to SwipeLab</Text>
        <Text style={styles.subtitle}>Swipe • Label • Improve Research</Text>

        <TextInput
          placeholder="Username"
          value={username}
          onChangeText={setUsername}
          style={styles.input}
          placeholderTextColor="#888"
        />
        <TextInput
          placeholder="Password"
          value={password}
          onChangeText={setPassword}
          style={styles.input}
          secureTextEntry
          placeholderTextColor="#888"
          onSubmitEditing={handleLogin}
        />
        {error ? <Text style={styles.error}>{error}</Text> : null}

        <TouchableOpacity style={styles.loginButton} onPress={handleLogin} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.loginButtonText}>Login</Text>}
        </TouchableOpacity>

        <TouchableOpacity style={[styles.loginButton, styles.researcherButton]} onPress={handleExternalLogin} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.loginButtonText}>Login as Researcher</Text>}
        </TouchableOpacity>

        <Text style={styles.orText}>OR</Text>

        <TouchableOpacity style={styles.googleButton} onPress={() => promptAsync()} disabled={!request}>
          <Image
            source={{ uri: "https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" }}
            style={styles.googleIcon}
          />
          <Text style={styles.googleText}>Continue with Google</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => setShowRegister(true)} style={{ marginTop: 20 }}>
          <Text style={styles.registerText}>Don&apos;t have an account? Register</Text>
        </TouchableOpacity>
      </View>

      {/* REGISTER FORM OVERLAY */}
      {showRegister && <RegisterForm onClose={() => setShowRegister(false)} />}
    </View>
  );
}

const styles = StyleSheet.create({
  screenContainer: { flex: 1, backgroundColor: '#fff' },
  // Web: center the card vertically on the page
  webScreenContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#fff",
    paddingHorizontal: 20,
  },
  // Web: contained card, shadow only for desktop feel
  webCard: {
    flex: 0,
    width: '100%',
    maxWidth: 460,
    borderRadius: 16,
    paddingVertical: 48,
    backgroundColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 16,
  },
  logo: { width: 140, height: 140, resizeMode: "contain", marginBottom: 30 },
  title: { fontSize: 28, fontWeight: "bold", marginBottom: 6 },
  subtitle: { fontSize: 16, color: "#777", marginBottom: 20 },
  input: { width: "85%", borderWidth: 1, borderColor: "#ccc", padding: 10, borderRadius: 8, color: "#000", marginBottom: 12, fontSize: 16 },
  loginButton: { width: "85%", backgroundColor: "#4B7BE5", padding: 12, borderRadius: 8, alignItems: "center", marginBottom: 12 },
  researcherButton: { backgroundColor: "#2E8B57" },
  loginButtonText: { color: "#fff", fontSize: 16, fontWeight: "600" },
  orText: { marginVertical: 10, fontSize: 14, color: "#555" },
  googleButton: { width: "85%", backgroundColor: "white", padding: 12, flexDirection: "row", alignItems: "center", borderRadius: 8, borderWidth: 1, borderColor: "#ccc", justifyContent: "center" },
  googleIcon: { width: 22, height: 22, marginRight: 10 },
  googleText: { fontSize: 16, fontWeight: "600" },
  error: { color: "red", marginBottom: 8 },
  registerText: { color: "#4B7BE5", textAlign: "center", fontSize: 14 },
});
