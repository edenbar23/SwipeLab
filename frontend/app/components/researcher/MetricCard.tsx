import React from "react";
import { StyleSheet, Text, View } from "react-native";

type MetricCardProps = {
    label: string;
    value: string | number;
    subtitle?: string;
    variant?: "primary" | "success" | "warning" | "danger";
    icon?: string;
};

export default function MetricCard({
    label,
    value,
    subtitle,
    variant = "primary",
}: MetricCardProps) {
    const variantStyles = {
        primary: { backgroundColor: "#EFF6FF", borderColor: "#3B82F6" },
        success: { backgroundColor: "#F0FDF4", borderColor: "#10B981" },
        warning: { backgroundColor: "#FFFBEB", borderColor: "#F59E0B" },
        danger: { backgroundColor: "#FEF2F2", borderColor: "#EF4444" },
    };

    const valueColors = {
        primary: "#1E40AF",
        success: "#047857",
        warning: "#D97706",
        danger: "#DC2626",
    };

    return (
        <View
            style={[
                styles.card,
                variantStyles[variant],
                { borderColor: variantStyles[variant].borderColor },
            ]}
        >
            <Text style={styles.label}>{label}</Text>
            <Text style={[styles.value, { color: valueColors[variant] }]}>
                {value}
            </Text>
            {subtitle && <Text style={styles.subtitle}>{subtitle}</Text>}
        </View>
    );
}

const styles = StyleSheet.create({
    card: {
        padding: 12,
        borderRadius: 12,
        borderWidth: 2,
        marginBottom: 12,
        height: 115,
        justifyContent: "center",
    },
    label: {
        fontSize: 10,
        fontWeight: "600",
        color: "#6B7280",
        marginBottom: 6,
        textTransform: "uppercase",
        letterSpacing: 0.3,
    },
    value: {
        fontSize: 28,
        fontWeight: "700",
        marginBottom: 4,
    },
    subtitle: {
        fontSize: 12,
        color: "#9CA3AF",
        marginTop: 2,
    },
});
