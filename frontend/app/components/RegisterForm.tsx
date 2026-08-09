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
import { Ionicons } from '@expo/vector-icons';
import { apiFetch } from "@/api/apiFetch";
import { useAuthStore } from "@/stores/authStore";
import { useModeStore } from "@/stores/modeStore";
import { API_ENDPOINTS } from '@/api/apiEndpoints';


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

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d\s])(?!.*\s).{8,}$/;
    if (!passwordRegex.test(password)) {
      setError("Password does not meet complexity requirements.");
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
        if (userRole === "researcher") {
          setMode("researcher");
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
        <TouchableOpacity style={styles.closeButton} onPress={onClose}>
          <Ionicons name="close" size={24} color="#000000" />
        </TouchableOpacity>

        {!success ? (
          <>
            <Text style={styles.title}>Register</Text>

            <TextInput
              placeholder="Username (e.g., jdoe23)"
              value={username}
              onChangeText={setUsername}
              style={styles.input}
              placeholderTextColor="#333"
            />
            <TextInput
              placeholder="Email address"
              value={email}
              onChangeText={setEmail}
              style={styles.input}
              keyboardType="email-address"
              autoCapitalize="none"
              placeholderTextColor="#333"
            />
            <TextInput
              placeholder="Display Name (e.g., John Doe)"
              value={displayName}
              onChangeText={setDisplayName}
              style={styles.input}
              placeholderTextColor="#333"
            />
            <TextInput
              placeholder="Create a strong password"
              value={password}
              onChangeText={setPassword}
              style={styles.input}
              secureTextEntry
              placeholderTextColor="#333"
            />
            <Text style={{ fontSize: 13, color: "#000", marginBottom: 16, marginTop: -6, marginLeft: 4 }}>
              Must be at least 8 chars, include an uppercase, a lowercase, a number, a special symbol, and no spaces.
            </Text>
            
            <TextInput
              placeholder="Confirm your password"
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              style={styles.input}
              secureTextEntry
              placeholderTextColor="#333"
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
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 1000,
  },
  container: {
    width: "90%",
    maxWidth: 550,
    backgroundColor: "#fff",
    borderRadius: 24,
    paddingHorizontal: 24,
    paddingTop: 36,
    paddingBottom: 24,
    shadowColor: "#000",
    shadowOpacity: 0.3,
    shadowRadius: 15,
    elevation: 8,
    position: 'relative',
  },
  closeButton: {
    position: 'absolute',
    top: 16,
    right: 16,
    zIndex: 10,
    padding: 4,
  },
  title: { fontSize: 28, fontWeight: "900", marginBottom: 24, textAlign: "center", color: "#000000" },
  input: {
    backgroundColor: "#f3f4f6", // Soft grey background
    padding: 14,
    borderRadius: 12,
    marginBottom: 16,
    fontSize: 16,
    color: "#000000", // Darker text
  },
  registerButton: {
    backgroundColor: "#4B7BE5",
    padding: 14,
    borderRadius: 12,
    alignItems: "center",
    marginTop: 8,
  },
  registerText: { color: "#fff", fontWeight: "bold", fontSize: 18 },
  error: { color: "red", marginBottom: 12, textAlign: "center", fontWeight: "500" },
});
