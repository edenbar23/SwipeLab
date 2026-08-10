import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { useThemeStore } from '@/stores/themeStore';
import type { ActivitySummary } from '@/types/analyticsTypes';

type Window = {
  label: string;
  data: ActivitySummary;
};

type Props = {
  today: ActivitySummary;
  thisWeek: ActivitySummary;
  thisMonth: ActivitySummary;
};

export default function TimeWindowCards({ today, thisWeek, thisMonth }: Props) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const isDark = theme === 'dark';

  const windows: Window[] = [
    { label: 'Today',      data: today },
    { label: 'This Week',  data: thisWeek },
    { label: 'This Month', data: thisMonth },
  ];

  return (
    <View style={styles.row}>
      {windows.map(({ label, data }) => (
        <View
          key={label}
          style={[
            styles.card,
            {
              backgroundColor: isDark ? themeColors.card : '#EFF6FF',
              borderColor: isDark ? themeColors.border : '#BFDBFE',
            },
          ]}
        >
          <Text style={[styles.windowLabel, { color: themeColors.textSecondary }]}>
            {label}
          </Text>

          <View style={styles.row2}>
            <Text style={styles.icon}>🔬</Text>
            <View>
              <Text style={[styles.bigNum, { color: '#1E40AF' }]}>
                {data.classifications.toLocaleString()}
              </Text>
              <Text style={[styles.metaLabel, { color: themeColors.textSecondary }]}>
                classifications
              </Text>
            </View>
          </View>

          <View style={[styles.divider, { backgroundColor: isDark ? themeColors.border : '#DBEAFE' }]} />

          <StatRow icon="🖼️" value={data.uniqueImages} label="images" color={themeColors.textSecondary} />
          <StatRow icon="👤" value={data.uniqueUsers}  label="users"  color={themeColors.textSecondary} />
          <StatRow icon="📋" value={data.uniqueTasks}  label="tasks"  color={themeColors.textSecondary} />
        </View>
      ))}
    </View>
  );
}

function StatRow({
  icon,
  value,
  label,
  color,
}: {
  icon: string;
  value: number;
  label: string;
  color: string;
}) {
  return (
    <View style={styles.statRow}>
      <Text style={styles.statIcon}>{icon}</Text>
      <Text style={[styles.statValue, { color }]}>
        {value.toLocaleString()} {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 8,
  },
  card: {
    flex: 1,
    borderRadius: 14,
    borderWidth: 1,
    padding: 12,
    gap: 6,
  },
  windowLabel: {
    fontSize: 10,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  row2: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  icon: {
    fontSize: 18,
  },
  bigNum: {
    fontSize: 22,
    fontWeight: '800',
    lineHeight: 24,
  },
  metaLabel: {
    fontSize: 9,
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },
  divider: {
    height: 1,
    marginVertical: 4,
  },
  statRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  statIcon: {
    fontSize: 11,
  },
  statValue: {
    fontSize: 11,
    fontWeight: '500',
  },
});
