import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '@/stores/themeStore';
import type { LabelDistributionPoint } from '@/types/analyticsTypes';

type Props = {
  data: LabelDistributionPoint[];
};

const LABEL_CONFIG: Record<string, { color: string; emoji: string }> = {
  YES:       { color: '#10B981', emoji: '✅' },
  NO:        { color: '#EF4444', emoji: '❌' },
  DONT_KNOW: { color: '#9CA3AF', emoji: '❓' },
  TRASH:     { color: '#F59E0B', emoji: '🗑️' },
};

export default function LabelDistributionBar({ data }: Props) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const total = data.reduce((s, p) => s + p.count, 0);

  if (total === 0) {
    return (
      <View
        style={[
          styles.empty,
          { borderColor: isDark ? themeColors.border : '#DBEAFE' },
        ]}
      >
        <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>
          No classifications yet
        </Text>
      </View>
    );
  }

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: isDark ? themeColors.card : '#F8FAFF',
          borderColor: isDark ? themeColors.border : '#DBEAFE',
        },
      ]}
    >
      {/* Total header */}
      <View style={styles.headerRow}>
        <Text style={[styles.headerLabel, { color: themeColors.textSecondary }]}>
          Label distribution
        </Text>
        <Text style={[styles.totalBadge, { color: themeColors.text }]}>
          {total.toLocaleString()} total
        </Text>
      </View>

      {/* Stacked bar */}
      <View style={styles.stackedBar}>
        {data.map((point) => {
          const pct = total > 0 ? (point.count / total) * 100 : 0;
          if (pct < 0.5) return null; // skip invisible slivers
          const cfg = LABEL_CONFIG[point.label] ?? { color: '#6B7280', emoji: '•' };
          return (
            <View
              key={point.label}
              style={[
                styles.segment,
                { width: `${pct}%` as any, backgroundColor: cfg.color },
              ]}
            />
          );
        })}
      </View>

      {/* Legend pills */}
      <View style={styles.legendRow}>
        {data.map((point) => {
          const cfg = LABEL_CONFIG[point.label] ?? { color: '#6B7280', emoji: '•' };
          const pct = total > 0 ? (point.count / total) * 100 : 0;
          return (
            <View key={point.label} style={styles.pill}>
              <Text style={styles.pillEmoji}>{cfg.emoji}</Text>
              <View>
                <Text style={[styles.pillLabel, { color: themeColors.textSecondary }]}>
                  {point.label.replace('_', ' ')}
                </Text>
                <Text style={[styles.pillValue, { color: cfg.color }]}>
                  {pct.toFixed(1)}%
                </Text>
                <Text style={[styles.pillCount, { color: themeColors.textSecondary }]}>
                  ({point.count.toLocaleString()})
                </Text>
              </View>
            </View>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: 14,
    borderWidth: 1,
    padding: 14,
    marginBottom: 12,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  headerLabel: {
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },
  totalBadge: {
    fontSize: 13,
    fontWeight: '700',
  },
  stackedBar: {
    flexDirection: 'row',
    height: 14,
    borderRadius: 7,
    overflow: 'hidden',
    marginBottom: 12,
    backgroundColor: '#E5E7EB',
  },
  segment: {
    height: '100%',
  },
  legendRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  pill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  pillEmoji: {
    fontSize: 18,
  },
  pillLabel: {
    fontSize: 9,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },
  pillValue: {
    fontSize: 15,
    fontWeight: '800',
  },
  pillCount: {
    fontSize: 10,
  },
  empty: {
    borderRadius: 14,
    borderWidth: 1,
    borderStyle: 'dashed',
    padding: 20,
    alignItems: 'center',
    marginBottom: 12,
  },
  emptyText: {
    fontSize: 13,
  },
});
