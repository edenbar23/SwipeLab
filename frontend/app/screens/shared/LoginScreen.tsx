import * as Google from "expo-auth-session/providers/google";
import * as WebBrowser from "expo-web-browser";
import React, { useEffect, useState } from "react";
import { ActivityIndicator, Image, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { apiFetch } from "../../api/apiFetch";
import RegisterForm from "../../components/RegisterForm";
import { useAuthStore } from "../../stores/authStore";


WebBrowser.maybeCompleteAuthSession();

export default function LoginScreen() {
  const setAuth = useAuthStore((s) => s.setAuth);
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
      setAuth(token, role);
    }
  }, [response]);

 const handleLogin = async () => {
  setLoading(true);
  setError("");

  try {
    const res = await apiFetch("/api/v1/auth/login", {
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
      setError("Invalid username or password");
      return;
    }

    const data = await res.json();

    // data comes from auth.mock.ts
    setAuth(data.accessToken, data.user.role);
  } catch (e) {
    setError("Something went wrong. Please try again.");
  } finally {
    setLoading(false);
  }
};


  return (
    <View style={styles.screenContainer}>
      {/* LOGIN SCREEN */}
      <View style={[styles.container, showRegister && { opacity: 0.75 }]}>
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
        />
        {error ? <Text style={styles.error}>{error}</Text> : null}

        <TouchableOpacity style={styles.loginButton} onPress={handleLogin} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.loginButtonText}>Login</Text>}
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
  screenContainer: { flex: 1 },
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#fff",
    paddingHorizontal: 20,
  },
  logo: { width: 140, height: 140, resizeMode: "contain", marginBottom: 30 },
  title: { fontSize: 28, fontWeight: "bold", marginBottom: 6 },
  subtitle: { fontSize: 16, color: "#777", marginBottom: 20 },
  input: { width: "85%", borderWidth: 1, borderColor: "#ccc", padding: 10, borderRadius: 8, color: "#000", marginBottom: 12 },
  loginButton: { width: "85%", backgroundColor: "#4B7BE5", padding: 12, borderRadius: 8, alignItems: "center", marginBottom: 12 },
  loginButtonText: { color: "#fff", fontSize: 16, fontWeight: "600" },
  orText: { marginVertical: 10, fontSize: 14, color: "#555" },
  googleButton: { width: "85%", backgroundColor: "white", padding: 12, flexDirection: "row", alignItems: "center", borderRadius: 8, borderWidth: 1, borderColor: "#ccc", justifyContent: "center" },
  googleIcon: { width: 22, height: 22, marginRight: 10 },
  googleText: { fontSize: 16, fontWeight: "600" },
  error: { color: "red", marginBottom: 8 },
  registerText: { color: "#4B7BE5", textAlign: "center", fontSize: 14 },
});
