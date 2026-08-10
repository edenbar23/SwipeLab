import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '@/stores/themeStore';
import type { ConfidenceTrendPoint } from '@/types/analyticsTypes';

type Props = {
  data: ConfidenceTrendPoint[];
};

// Bar color based on credibility: red < 50, yellow 50-75, green > 75
// averageCredibility is on the 0–100 scale from the backend
function barColor(cred: number): string {
  if (cred >= 75) return '#10B981'; // green
  if (cred >= 50) return '#F59E0B'; // amber
  return '#EF4444';                 // red
}

export default function ConfidenceTrendChart({ data }: Props) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  if (!data || data.length === 0) {
    return (
      <View style={[styles.empty, { borderColor: isDark ? themeColors.border : '#DBEAFE' }]}>
        <Text style={[styles.emptyText, { color: themeColors.textSecondary }]}>
          No trend data yet
        </Text>
      </View>
    );
  }

  // Show last 30 points; truncate label to last 7 if more
  const points = data.slice(-30);
  // Normalise to 0–1 for chart math (backend sends 0–100)
  const normalised = points.map(p => ({
    ...p,
    norm: (p.averageCredibility ?? 0) / 100,
  }));
  const avgCredibility =
    normalised.reduce((sum, p) => sum + p.norm, 0) / normalised.length;

  // First and last date for the x-axis label
  const firstDate = points[0]?.date?.slice(5) ?? ''; // "MM-DD"
  const lastDate  = points[points.length - 1]?.date?.slice(5) ?? '';

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
      {/* Header */}
      <View style={styles.header}>
        <Text style={[styles.headerLabel, { color: themeColors.textSecondary }]}>
          30-day avg credibility
        </Text>
        <Text style={[styles.avgBadge, { color: barColor(avgCredibility) }]}>
          {(avgCredibility * 100).toFixed(0)}%
        </Text>
      </View>

      {/* Bars */}
      <View style={styles.chartArea}>
        {normalised.map((point, i) => {
          const height = Math.max(4, point.norm * 60);
          return (
            <View key={i} style={styles.barWrapper}>
              <View style={styles.barTrack}>
                <View
                  style={[
                    styles.bar,
                    {
                      height,
                      backgroundColor: barColor(point.averageCredibility),
                      opacity: 0.85,
                    },
                  ]}
                />
              </View>
            </View>
          );
        })}
      </View>

      {/* X-axis labels */}
      <View style={styles.xAxis}>
        <Text style={[styles.xLabel, { color: themeColors.textSecondary }]}>{firstDate}</Text>
        <View style={styles.legend}>
          <View style={[styles.legendDot, { backgroundColor: '#EF4444' }]} />
          <Text style={[styles.legendText, { color: themeColors.textSecondary }]}>&lt;50%</Text>
          <View style={[styles.legendDot, { backgroundColor: '#F59E0B' }]} />
          <Text style={[styles.legendText, { color: themeColors.textSecondary }]}>50-75%</Text>
          <View style={[styles.legendDot, { backgroundColor: '#10B981' }]} />
          <Text style={[styles.legendText, { color: themeColors.textSecondary }]}>&gt;75%</Text>
        </View>
        <Text style={[styles.xLabel, { color: themeColors.textSecondary }]}>{lastDate}</Text>
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
  header: {
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
  avgBadge: {
    fontSize: 18,
    fontWeight: '800',
  },
  chartArea: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    height: 64,
    gap: 2,
  },
  barWrapper: {
    flex: 1,
    height: 64,
    justifyContent: 'flex-end',
  },
  barTrack: {
    justifyContent: 'flex-end',
    height: 60,
  },
  bar: {
    borderRadius: 3,
    minHeight: 4,
  },
  xAxis: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 6,
  },
  xLabel: {
    fontSize: 10,
    fontWeight: '500',
  },
  legend: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  legendDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
  },
  legendText: {
    fontSize: 9,
    marginRight: 4,
  },
  empty: {
    borderRadius: 14,
    borderWidth: 1,
    borderStyle: 'dashed',
    padding: 24,
    alignItems: 'center',
    marginBottom: 12,
  },
  emptyText: {
    fontSize: 13,
  },
});
