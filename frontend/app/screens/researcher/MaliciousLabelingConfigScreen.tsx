import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import ScreenHeaderLayout from '@/components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import { useThemeStore } from '@/stores/themeStore';
import {
  useMaliciousLabelingConfig,
  useMaliciousLabelingAuditLog,
  useUpdateMaliciousLabelingConfig,
} from '@/api/queries';

// ── Types ─────────────────────────────────────────────────────────────────────

interface ConfigForm {
  maliciousThreshold: string;
  maliciousMinSamples: string;
  autoBanEnabled: boolean;
  minResponseTimeMs: string;
  researcherMinResponseTimeMs: string;
  suspiciousCountForStrike: string;
  slidingWindowMinutes: string;
  strikesForWarning1: string;
  strikesForWarning2: string;
  strikesForBan: string;
  warningCooldownMinutes: string;
}

interface FieldErrors {
  [key: string]: string;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const configToForm = (cfg: any): ConfigForm => ({
  maliciousThreshold:          String(cfg.maliciousThreshold ?? 15),
  maliciousMinSamples:         String(cfg.maliciousMinSamples ?? 20),
  autoBanEnabled:              cfg.autoBanEnabled ?? true,
  minResponseTimeMs:           String(cfg.minResponseTimeMs ?? 300),
  researcherMinResponseTimeMs: String(cfg.researcherMinResponseTimeMs ?? 150),
  suspiciousCountForStrike:    String(cfg.suspiciousCountForStrike ?? 3),
  slidingWindowMinutes:        String(cfg.slidingWindowMinutes ?? 10),
  strikesForWarning1:          String(cfg.strikesForWarning1 ?? 5),
  strikesForWarning2:          String(cfg.strikesForWarning2 ?? 10),
  strikesForBan:               String(cfg.strikesForBan ?? 15),
  warningCooldownMinutes:      String(cfg.warningCooldownMinutes ?? 30),
});

const validate = (form: ConfigForm): FieldErrors => {
  const errors: FieldErrors = {};
  const num = (v: string) => parseFloat(v);
  const int = (v: string) => parseInt(v, 10);

  if (isNaN(num(form.maliciousThreshold)) || num(form.maliciousThreshold) < 0 || num(form.maliciousThreshold) > 100)
    errors.maliciousThreshold = 'Must be 0 – 100';
  if (isNaN(int(form.maliciousMinSamples)) || int(form.maliciousMinSamples) < 1 || int(form.maliciousMinSamples) > 10000)
    errors.maliciousMinSamples = 'Must be 1 – 10 000';
  if (isNaN(int(form.minResponseTimeMs)) || int(form.minResponseTimeMs) < 50 || int(form.minResponseTimeMs) > 10000)
    errors.minResponseTimeMs = 'Must be 50 – 10 000 ms';
  if (isNaN(int(form.researcherMinResponseTimeMs)) || int(form.researcherMinResponseTimeMs) < 50 || int(form.researcherMinResponseTimeMs) > 10000)
    errors.researcherMinResponseTimeMs = 'Must be 50 – 10 000 ms';
  if (!errors.minResponseTimeMs && !errors.researcherMinResponseTimeMs &&
      int(form.researcherMinResponseTimeMs) > int(form.minResponseTimeMs))
    errors.researcherMinResponseTimeMs = 'Must be ≤ Regular min time';
  if (isNaN(int(form.suspiciousCountForStrike)) || int(form.suspiciousCountForStrike) < 1 || int(form.suspiciousCountForStrike) > 100)
    errors.suspiciousCountForStrike = 'Must be 1 – 100';
  if (isNaN(int(form.slidingWindowMinutes)) || int(form.slidingWindowMinutes) < 1 || int(form.slidingWindowMinutes) > 1440)
    errors.slidingWindowMinutes = 'Must be 1 – 1 440 min';
  if (isNaN(int(form.strikesForWarning1)) || int(form.strikesForWarning1) < 1)
    errors.strikesForWarning1 = 'Must be ≥ 1';
  if (!errors.strikesForWarning1 && int(form.strikesForWarning2) <= int(form.strikesForWarning1))
    errors.strikesForWarning2 = `Must be > Warning 1 (${form.strikesForWarning1})`;
  if (!errors.strikesForWarning2 && int(form.strikesForBan) <= int(form.strikesForWarning2))
    errors.strikesForBan = `Must be > Warning 2 (${form.strikesForWarning2})`;
  if (!errors.strikesForBan && int(form.strikesForBan) > 1000)
    errors.strikesForBan = 'Must be ≤ 1 000';
  if (isNaN(int(form.warningCooldownMinutes)) || int(form.warningCooldownMinutes) < 1 || int(form.warningCooldownMinutes) > 1440)
    errors.warningCooldownMinutes = 'Must be 1 – 1 440 min';
  return errors;
};

const formToPayload = (form: ConfigForm) => ({
  maliciousThreshold:          parseFloat(form.maliciousThreshold),
  maliciousMinSamples:         parseInt(form.maliciousMinSamples, 10),
  autoBanEnabled:              form.autoBanEnabled,
  minResponseTimeMs:           parseInt(form.minResponseTimeMs, 10),
  researcherMinResponseTimeMs: parseInt(form.researcherMinResponseTimeMs, 10),
  suspiciousCountForStrike:    parseInt(form.suspiciousCountForStrike, 10),
  slidingWindowMinutes:        parseInt(form.slidingWindowMinutes, 10),
  strikesForWarning1:          parseInt(form.strikesForWarning1, 10),
  strikesForWarning2:          parseInt(form.strikesForWarning2, 10),
  strikesForBan:               parseInt(form.strikesForBan, 10),
  warningCooldownMinutes:      parseInt(form.warningCooldownMinutes, 10),
});

// ── Main component ────────────────────────────────────────────────────────────

export default function MaliciousLabelingConfigScreen() {
  const navigation = useNavigation<any>();
  const { theme } = useThemeStore();
  const isDark = theme === 'dark';

  const { data: remoteConfig, isLoading, isError } = useMaliciousLabelingConfig();
  const { data: auditData } = useMaliciousLabelingAuditLog(0, 15);
  const updateMutation = useUpdateMaliciousLabelingConfig();

  const [form, setForm] = useState<ConfigForm | null>(null);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [saveError, setSaveError] = useState('');

  // Populate form once remote config arrives
  useEffect(() => {
    if (remoteConfig && !form) {
      setForm(configToForm(remoteConfig));
    }
  }, [remoteConfig]);

  const isDirty = form && remoteConfig
    ? JSON.stringify(formToPayload(form)) !== JSON.stringify(formToPayload(configToForm(remoteConfig)))
    : false;

  const handleSave = () => {
    if (!form) return;
    const fieldErrors = validate(form);
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) return;

    setSaveError('');
    setSaveSuccess(false);
    updateMutation.mutate(formToPayload(form), {
      onSuccess: () => { setSaveSuccess(true); setTimeout(() => setSaveSuccess(false), 3000); },
      onError: (err: any) => { setSaveError(err.message ?? 'Save failed'); },
    });
  };

  // ── Colours ────────────────────────────────────────────────────────────────

  const c = {
    bg:          isDark ? '#0f0f1a' : '#f4f6f9',
    card:        isDark ? '#1c1c2e' : '#ffffff',
    cardBorder:  isDark ? '#2e2e45' : '#e5e7eb',
    text:        isDark ? '#f1f1f1' : '#1a1a2e',
    textSub:     isDark ? '#9ca3af' : '#6b7280',
    inputBg:     isDark ? '#252538' : '#f9fafb',
    inputBorder: isDark ? '#3d3d5c' : '#d1d5db',
    accent:      '#6366f1',
    error:       '#ef4444',
    success:     '#22c55e',
  };

  // ── Sub-components ─────────────────────────────────────────────────────────

  const Field = ({
    label, fieldKey, unit, keyboardType = 'numeric',
  }: {
    label: string; fieldKey: keyof ConfigForm; unit?: string; keyboardType?: 'numeric' | 'decimal-pad';
  }) => (
    <View style={s.fieldRow}>
      <View style={s.fieldLabelWrap}>
        <Text style={[s.fieldLabel, { color: c.text }]}>{label}</Text>
        {unit && <Text style={[s.fieldUnit, { color: c.textSub }]}>{unit}</Text>}
      </View>
      <View style={[s.inputWrap, { backgroundColor: c.inputBg, borderColor: errors[fieldKey] ? c.error : c.inputBorder }]}>
        <TextInput
          style={[s.input, { color: c.text }]}
          value={form ? String(form[fieldKey]) : ''}
          onChangeText={v => { setForm(prev => prev ? { ...prev, [fieldKey]: v } : prev); setSaveError(''); }}
          keyboardType={keyboardType}
          placeholderTextColor={c.textSub}
        />
      </View>
      {errors[fieldKey] ? <Text style={[s.errorText, { color: c.error }]}>{errors[fieldKey]}</Text> : null}
    </View>
  );

  const SwitchField = ({ label, desc }: { label: string; desc: string }) => (
    <View style={[s.switchRow, { backgroundColor: c.card, borderColor: c.cardBorder }]}>
      <View style={s.switchTextWrap}>
        <Text style={[s.switchLabel, { color: c.text }]}>{label}</Text>
        <Text style={[s.switchDesc, { color: c.textSub }]}>{desc}</Text>
      </View>
      <Switch
        value={form?.autoBanEnabled ?? true}
        onValueChange={v => setForm(prev => prev ? { ...prev, autoBanEnabled: v } : prev)}
        trackColor={{ false: isDark ? '#374151' : '#d1d5db', true: c.accent }}
        thumbColor="#fff"
      />
    </View>
  );

  const Section = ({ title, icon, children }: { title: string; icon: string; children: React.ReactNode }) => (
    <View style={[s.section, { backgroundColor: c.card, borderColor: c.cardBorder }]}>
      <View style={s.sectionHeader}>
        <Ionicons name={icon as any} size={18} color={c.accent} style={{ marginRight: 8 }} />
        <Text style={[s.sectionTitle, { color: c.text }]}>{title}</Text>
      </View>
      {children}
    </View>
  );

  // ── Render ─────────────────────────────────────────────────────────────────

  if (isLoading || !form) {
    return (
      <View style={[s.centered, { backgroundColor: c.bg }]}>
        <ActivityIndicator size="large" color={c.accent} />
        <Text style={{ color: c.textSub, marginTop: 12 }}>Loading configuration…</Text>
      </View>
    );
  }

  if (isError) {
    return (
      <View style={[s.centered, { backgroundColor: c.bg }]}>
        <Ionicons name="alert-circle-outline" size={48} color={c.error} />
        <Text style={{ color: c.error, marginTop: 12 }}>Failed to load configuration.</Text>
      </View>
    );
  }

  return (
    <ScreenHeaderLayout
      leftIcon={require('../../../assets/images/settings.png')}
      leftTitle="Malicious Labeling Config"
      rightIcon={require('../../../assets/images/settings.png')}
      rightTitle="Settings"
      onRightPress={() => navigation.navigate('UserSettings')}
      contentContainerStyle={{ padding: 0 }}
    >
      <ScrollView
        style={{ backgroundColor: c.bg }}
        contentContainerStyle={s.scrollContent}
        showsVerticalScrollIndicator={false}
      >

        {/* ── Feedback banners ─────────────────────────────────── */}
        {saveSuccess && (
          <View style={[s.banner, { backgroundColor: '#dcfce7', borderColor: '#86efac' }]}>
            <Ionicons name="checkmark-circle" size={18} color={c.success} />
            <Text style={[s.bannerText, { color: '#166534' }]}>Configuration saved successfully.</Text>
          </View>
        )}
        {saveError !== '' && (
          <View style={[s.banner, { backgroundColor: '#fee2e2', borderColor: '#fca5a5' }]}>
            <Ionicons name="alert-circle" size={18} color={c.error} />
            <Text style={[s.bannerText, { color: '#991b1b' }]}>{saveError}</Text>
          </View>
        )}

        {/* ── Credibility-based detection ─────────────────────── */}
        <Section title="Credibility Detection" icon="shield-checkmark-outline">
          <Field label="Malicious Threshold" fieldKey="maliciousThreshold" unit="score 0–100" keyboardType="decimal-pad" />
          <Field label="Min Samples" fieldKey="maliciousMinSamples" unit="classifications" />
        </Section>

        {/* ── Auto-ban toggle ──────────────────────────────────── */}
        <SwitchField
          label="Auto-Ban"
          desc={
            form.autoBanEnabled
              ? 'Users are automatically banned when strikes reach the ban threshold.'
              : 'Auto-ban is OFF — strikes accumulate but no ban is issued.'
          }
        />

        {/* ── Fraud detection (speed-based) ───────────────────── */}
        <Section title="Fraud Detection — Speed Thresholds" icon="timer-outline">
          <Field label="Min Response Time (Regular)" fieldKey="minResponseTimeMs" unit="ms" />
          <Field label="Min Response Time (Researcher)" fieldKey="researcherMinResponseTimeMs" unit="ms" />
          <Field label="Suspicious Count / Strike" fieldKey="suspiciousCountForStrike" unit="events" />
          <Field label="Sliding Window" fieldKey="slidingWindowMinutes" unit="min" />
        </Section>

        {/* ── Escalation ladder ────────────────────────────────── */}
        <Section title="Escalation Ladder" icon="trending-up-outline">
          <Field label="Strikes for Warning 1" fieldKey="strikesForWarning1" unit="strikes" />
          <Field label="Strikes for Warning 2" fieldKey="strikesForWarning2" unit="strikes" />
          <Field label="Strikes for Ban" fieldKey="strikesForBan" unit="strikes" />
          <Field label="Warning Cooldown" fieldKey="warningCooldownMinutes" unit="min" />
        </Section>

        {/* ── Save button ──────────────────────────────────────── */}
        <TouchableOpacity
          style={[
            s.saveButton,
            { backgroundColor: isDirty ? c.accent : (isDark ? '#374151' : '#d1d5db') },
          ]}
          onPress={handleSave}
          disabled={!isDirty || updateMutation.isPending}
        >
          {updateMutation.isPending
            ? <ActivityIndicator size="small" color="#fff" />
            : <Text style={s.saveButtonText}>{isDirty ? 'Save Changes' : 'No Changes'}</Text>
          }
        </TouchableOpacity>

        {/* ── Audit log ────────────────────────────────────────── */}
        <View style={[s.section, { backgroundColor: c.card, borderColor: c.cardBorder }]}>
          <View style={s.sectionHeader}>
            <Ionicons name="document-text-outline" size={18} color={c.accent} style={{ marginRight: 8 }} />
            <Text style={[s.sectionTitle, { color: c.text }]}>Recent Changes</Text>
          </View>

          {(!auditData?.content || auditData.content.length === 0) ? (
            <Text style={[s.auditEmpty, { color: c.textSub }]}>No changes recorded yet.</Text>
          ) : (
            auditData.content.map((entry: any) => (
              <View key={entry.id} style={[s.auditRow, { borderBottomColor: c.cardBorder }]}>
                <View style={s.auditMeta}>
                  <Text style={[s.auditKey, { color: c.accent }]}>{entry.configKey}</Text>
                  <Text style={[s.auditBy, { color: c.textSub }]}>
                    {entry.changedBy} · {new Date(entry.changedAt).toLocaleString()}
                  </Text>
                </View>
                <Text style={[s.auditDiff, { color: c.text }]}>
                  <Text style={{ color: c.error }}>{entry.previousValue ?? '—'}</Text>
                  {' → '}
                  <Text style={{ color: c.success }}>{entry.newValue}</Text>
                </Text>
              </View>
            ))
          )}
        </View>

      </ScrollView>
    </ScreenHeaderLayout>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const s = StyleSheet.create({
  centered:       { flex: 1, justifyContent: 'center', alignItems: 'center' },
  scrollContent:  { padding: 16, paddingBottom: 60 },
  banner: {
    flexDirection: 'row', alignItems: 'center', gap: 8,
    borderWidth: 1, borderRadius: 10, padding: 12, marginBottom: 12,
  },
  bannerText:     { fontSize: 14, fontWeight: '500' },

  // Section card
  section: {
    borderWidth: 1, borderRadius: 14, padding: 16, marginBottom: 14,
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06, shadowRadius: 4, elevation: 2,
  },
  sectionHeader:  { flexDirection: 'row', alignItems: 'center', marginBottom: 14 },
  sectionTitle:   { fontSize: 15, fontWeight: '700', letterSpacing: 0.3 },

  // Fields
  fieldRow:       { marginBottom: 12 },
  fieldLabelWrap: { flexDirection: 'row', alignItems: 'center', marginBottom: 6, gap: 6 },
  fieldLabel:     { fontSize: 14, fontWeight: '600' },
  fieldUnit:      { fontSize: 12 },
  inputWrap: {
    borderWidth: 1, borderRadius: 10, paddingHorizontal: 12,
    paddingVertical: 9, flexDirection: 'row', alignItems: 'center',
  },
  input:          { flex: 1, fontSize: 15 },
  errorText:      { fontSize: 12, marginTop: 4 },

  // Switch row
  switchRow: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    borderWidth: 1, borderRadius: 14, padding: 16, marginBottom: 14,
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06, shadowRadius: 4, elevation: 2,
  },
  switchTextWrap: { flex: 1, marginRight: 12 },
  switchLabel:    { fontSize: 15, fontWeight: '700', marginBottom: 2 },
  switchDesc:     { fontSize: 13 },

  // Save button
  saveButton: {
    borderRadius: 12, paddingVertical: 14, alignItems: 'center',
    marginBottom: 20, marginTop: 4,
    shadowColor: '#6366f1', shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.25, shadowRadius: 6, elevation: 4,
  },
  saveButtonText: { color: '#fff', fontWeight: '700', fontSize: 16 },

  // Audit log
  auditEmpty:  { fontSize: 14, textAlign: 'center', paddingVertical: 12 },
  auditRow: {
    paddingVertical: 10, borderBottomWidth: 1,
  },
  auditMeta:  { marginBottom: 4 },
  auditKey:   { fontSize: 13, fontWeight: '700' },
  auditBy:    { fontSize: 12 },
  auditDiff:  { fontSize: 13 },
});
