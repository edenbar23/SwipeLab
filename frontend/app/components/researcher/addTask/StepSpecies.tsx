import React, { useMemo } from 'react';
import { StyleSheet, Text, TouchableOpacity, View, ScrollView, Platform, ActivityIndicator } from 'react-native';
import { Colors } from '../../../../constants/theme';
import { useThemeStore } from '../../../stores/themeStore';
import { SpeciesRefImage, StepSpeciesProps } from './addTaskTypes';
import MultiSelect from '../../../components/ui/MultiSelect';
import SpeciesImagePicker from './SpeciesImagePicker';

export default function StepSpecies({
  formData,
  onUpdate,
  onNext,
  onBack,
  availableSpecies = [],
  optionsLoading = false,
  poolImages = {},
  poolImagesLoading = false,
}: StepSpeciesProps) {
  const { theme } = useThemeStore();
  const c = Colors[theme as keyof typeof Colors];
  const isWeb = Platform.OS === 'web';

  // Every selected species must have 1-3 reference images
  const canProceed = useMemo(() => {
    if (formData.speciesList.length === 0) return false;
    return formData.speciesList.every((id) => {
      const imgs = formData.speciesReferenceImages[id] ?? [];
      return imgs.length >= 1 && imgs.length <= 3;
    });
  }, [formData.speciesList, formData.speciesReferenceImages]);

  const handleImagesChange = (speciesId: string, images: SpeciesRefImage[]) => {
    onUpdate({
      speciesReferenceImages: {
        ...formData.speciesReferenceImages,
        [speciesId]: images,
      },
    });
  };

  // List of selected species with their label — for rendering pickers below the multiselect
  const selectedSpeciesDetails = useMemo(() =>
    formData.speciesList.map((id) => ({
      id,
      label: availableSpecies.find((s) => String(s.id) === id)?.label ?? id,
    })),
    [formData.speciesList, availableSpecies]
  );

  return (
    <View style={[styles.container, isWeb && styles.containerWeb]}>
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={[styles.heading, { color: c.text }, isWeb && styles.headingWeb]}>
          Choose species to label
        </Text>
        <Text style={[styles.subtitle, { color: c.textSecondary }, isWeb && styles.subtitleWeb]}>
          Add the species classifiers will identify, then attach 1–3 reference images per species.
        </Text>

        <MultiSelect
          options={availableSpecies}
          selectedIds={formData.speciesList}
          onToggle={(id) => {
            const sid = id as string;
            const newSpeciesList = formData.speciesList.includes(sid)
              ? formData.speciesList.filter((s) => s !== sid)
              : [...formData.speciesList, sid];
            onUpdate({ speciesList: newSpeciesList });
          }}
          placeholder="Search species..."
          loading={optionsLoading}
          emptyOnNoSearch={true}
        />

        {/* Reference image pickers — one per selected species */}
        {selectedSpeciesDetails.length > 0 && (
          <View style={styles.pickersSection}>
            <Text style={[styles.pickersHeading, { color: c.textSecondary }]}>
              REFERENCE IMAGES
            </Text>

            {poolImagesLoading && (
              <View style={styles.poolLoadingRow}>
                <ActivityIndicator size="small" color="#10B981" />
                <Text style={[styles.poolLoadingText, { color: c.textSecondary }]}>
                  Loading pool images...
                </Text>
              </View>
            )}

            {selectedSpeciesDetails.map(({ id, label }) => (
              <SpeciesImagePicker
                key={id}
                speciesId={id}
                speciesLabel={label}
                selectedImages={formData.speciesReferenceImages[id] ?? []}
                poolImages={poolImages[id] ?? []}
                poolLoading={poolImagesLoading}
                onImagesChange={handleImagesChange}
              />
            ))}
          </View>
        )}
        
        {/* Consensus Settings */}
        <View style={styles.consensusSection}>
          <Text style={[styles.pickersHeading, { color: c.textSecondary }]}>
            CONSENSUS SETTINGS
          </Text>
          <View style={[styles.consensusContainer, { backgroundColor: c.card, borderColor: c.border }]}>
            <View style={styles.consensusTextWrapper}>
              <Text style={[styles.consensusLabel, { color: c.text }]}>Threshold</Text>
              <Text style={[styles.consensusHint, { color: c.textSecondary }]}>
                Cumulative score required for an image to reach consensus (3 - 20). 
                E.g., 3 requires 3 expert classifications.
              </Text>
            </View>
            <View style={styles.stepperContainer}>
              <TouchableOpacity
                style={[styles.stepperBtn, { borderColor: c.border, backgroundColor: c.background }]}
                onPress={() => {
                  const val = Math.max(3, (formData.consensusThreshold || 3) - 1);
                  onUpdate({ consensusThreshold: val });
                }}
                disabled={(formData.consensusThreshold || 3) <= 3}
              >
                <Text style={[styles.stepperBtnText, { color: (formData.consensusThreshold || 3) <= 3 ? c.textSecondary : c.text }]}>-</Text>
              </TouchableOpacity>
              <Text style={[styles.stepperValue, { color: c.text }]}>{formData.consensusThreshold || 3}</Text>
              <TouchableOpacity
                style={[styles.stepperBtn, { borderColor: c.border, backgroundColor: c.background }]}
                onPress={() => {
                  const val = Math.min(20, (formData.consensusThreshold || 3) + 1);
                  onUpdate({ consensusThreshold: val });
                }}
                disabled={(formData.consensusThreshold || 3) >= 20}
              >
                <Text style={[styles.stepperBtnText, { color: (formData.consensusThreshold || 3) >= 20 ? c.textSecondary : c.text }]}>+</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>

      </ScrollView>

      <View style={[styles.footer, isWeb && styles.footerWeb]}>
        {/* Validation hint */}
        {formData.speciesList.length > 0 && !canProceed && (
          <Text style={styles.validationHint}>
            Each species needs 1–3 reference images before continuing.
          </Text>
        )}

        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.backButton, { borderColor: c.border }]}
            onPress={onBack}
          >
            <Text style={[styles.backButtonText, { color: c.text }]}>← Back</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.nextButton, !canProceed && styles.buttonDisabled]}
            onPress={onNext}
            disabled={!canProceed}
          >
            <Text style={styles.nextButtonText}>Next →</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:         { flex: 1, paddingVertical: 16 },
  containerWeb:      { paddingVertical: 8 },
  scrollContent:     { paddingBottom: 16 },
  heading:           { fontSize: 22, fontWeight: '700', marginBottom: 8 },
  headingWeb:        { fontSize: 20, marginBottom: 6 },
  subtitle:          { fontSize: 14, marginBottom: 24, lineHeight: 20 },
  subtitleWeb:       { marginBottom: 16 },
  pickersSection:    { marginTop: 20, gap: 4 },
  pickersHeading:    { fontSize: 11, fontWeight: '700', letterSpacing: 0.8, marginBottom: 8 },
  poolLoadingRow:    { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 },
  poolLoadingText:   { fontSize: 13 },
  footer:            { paddingTop: 16 },
  footerWeb:         { paddingTop: 12 },
  validationHint:    { color: '#f59e0b', fontSize: 12, textAlign: 'center', marginBottom: 8 },
  buttonRow:         { flexDirection: 'row', gap: 12 },
  backButton:        { flex: 1, paddingVertical: 14, borderRadius: 12, alignItems: 'center', borderWidth: 1 },
  backButtonText:    { fontWeight: '700', fontSize: 16 },
  nextButton:        { flex: 1, backgroundColor: '#10B981', paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
  buttonDisabled:    { backgroundColor: '#94D3B3' },
  nextButtonText:    { color: '#fff', fontWeight: '700', fontSize: 16 },
  
  consensusSection:  { marginTop: 32, paddingBottom: 16 },
  consensusContainer:{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16, borderRadius: 12, borderWidth: 1, gap: 16 },
  consensusTextWrapper: { flex: 1 },
  consensusLabel:    { fontSize: 16, fontWeight: '600', marginBottom: 4 },
  consensusHint:     { fontSize: 13, lineHeight: 18 },
  stepperContainer:  { flexDirection: 'row', alignItems: 'center', gap: 12 },
  stepperBtn:        { width: 36, height: 36, borderRadius: 18, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  stepperBtnText:    { fontSize: 18, fontWeight: '700' },
  stepperValue:      { fontSize: 18, fontWeight: '700', minWidth: 24, textAlign: 'center' },
});
