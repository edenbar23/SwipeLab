// components/RegisterForm/index.tsx
import React, { useState } from "react";
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { apiFetch } from "../api/apiFetch";
import { useAuthStore } from "../stores/authStore";
import { useModeStore } from "../stores/modeStore";
import { API_ENDPOINTS } from '../api/apiEndpoints';


interface Props {
  onClose: () => void;
}

export default function RegisterForm({ onClose }: Props) {
  const setAuth = useAuthStore((s) => s.setAuth);
  const setMode = useModeStore((s) => s.setMode);

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleRegister = async () => {
    setLoading(true);
    setError("");
    setSuccess("");

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      setLoading(false);
      return;
    }

    try {
      // TODO: replace with real API call
      // const token = "mock-jwt-token";
      const role = "USER";

      const response = await apiFetch(API_ENDPOINTS.AUTH.REGISTER, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          email,
          password,
          displayName,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Registration failed");
      }

      const data = await response.json();
      const { accessToken, refreshToken, user, message } = data;

      if (accessToken) {
        // Ensure we treat the user object correctly
        // Assuming user object has username, email, displayName, and possibly role
        const userRole = user?.role || "USER";

        setAuth(accessToken, userRole, refreshToken);

        // Set default mode based on role
        if (userRole === "ADMIN") {
          setMode("ADMIN");
        } else {
          setMode("USER");
        }
        onClose();
      } else {
        setSuccess(message || "Registration successful! A verification link has been sent to your email.");
      }
    } catch (err: any) {
      setError(err.message || "Registration failed. Try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.container}>
        {!success ? (
          <>
            <Text style={styles.title}>Register</Text>

            <TextInput
              placeholder="Username"
              value={username}
              onChangeText={setUsername}
              style={styles.input}
              placeholderTextColor="#888"
            />
            <TextInput
              placeholder="Email"
              value={email}
              onChangeText={setEmail}
              style={styles.input}
              keyboardType="email-address"
              autoCapitalize="none"
              placeholderTextColor="#888"
            />
            <TextInput
              placeholder="Display Name"
              value={displayName}
              onChangeText={setDisplayName}
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
            <TextInput
              placeholder="Confirm Password"
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              style={styles.input}
              secureTextEntry
              placeholderTextColor="#888"
            />

            {error ? <Text style={styles.error}>{error}</Text> : null}

            <TouchableOpacity
              style={styles.registerButton}
              onPress={handleRegister}
              disabled={loading}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.registerText}>Register</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity onPress={onClose} style={{ marginTop: 10 }}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
          </>
        ) : (
          <View style={{ alignItems: "center", paddingVertical: 20 }}>
            <Text style={{ fontSize: 40, marginBottom: 15 }}>✉️</Text>
            <Text style={{ fontSize: 22, fontWeight: "bold", textAlign: "center", marginBottom: 10 }}>Check Your Email</Text>
            <Text style={{ fontSize: 16, color: "#555", textAlign: "center", marginBottom: 30, lineHeight: 22 }}>
              We've sent a verification link to <Text style={{fontWeight: "bold"}}>{email}</Text>. Please check your inbox to activate your account.
            </Text>
            
            <TouchableOpacity
              style={[styles.registerButton, { width: "100%" }]}
              onPress={onClose}
            >
              <Text style={styles.registerText}>Back to Login</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(255,255,255,0.25)", // 75% opacity white behind
    justifyContent: "center",
    alignItems: "center",
  },
  container: {
    width: "85%",
    backgroundColor: "#fff",
    borderRadius: 20,
    padding: 20,
    shadowColor: "#000",
    shadowOpacity: 0.2,
    shadowRadius: 10,
    elevation: 5,
  },
  title: { fontSize: 24, fontWeight: "bold", marginBottom: 12, textAlign: "center" },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    padding: 10,
    borderRadius: 8,
    marginBottom: 12,
    fontSize: 16,
  },
  registerButton: {
    backgroundColor: "#4B7BE5",
    padding: 12,
    borderRadius: 12,
    alignItems: "center",
  },
  registerText: { color: "#fff", fontWeight: "600", fontSize: 16 },
  error: { color: "red", marginBottom: 8, textAlign: "center" },
  cancelText: { color: "#4B7BE5", textAlign: "center", marginTop: 5 },
});
