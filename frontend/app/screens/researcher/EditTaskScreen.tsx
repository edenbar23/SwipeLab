import { NativeStackScreenProps } from "@react-navigation/native-stack";
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Keyboard,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View
} from "react-native";

import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '@/api/apiEndpoints';
import { apiFetch } from "@/api/apiFetch";
import ScreenHeaderLayout from "@/components/layout/ScreenHeaderLayout";
import { useQueryClient } from "@tanstack/react-query";
import MultiSelect from "@/components/ui/MultiSelect";
import SpeciesImagePicker from "@/components/researcher/addTask/SpeciesImagePicker";
import { SpeciesRefImage } from "@/components/researcher/addTask/addTaskTypes";
import { useSpeciesPoolImages, QUERY_KEYS } from "@/api/queries";
import { researcherStackParamList } from "@/navigation/researcherStack.types";
import { useThemeStore } from '@/stores/themeStore';


type Props = NativeStackScreenProps<
  researcherStackParamList,
  "EditTask"
>;

export default function EditTaskScreen({ route, navigation }: Props) {
  const { taskId } = route.params;
  const queryClient = useQueryClient();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [targetSpecies, setTargetSpecies] = useState<string[]>([]);
  // Maps species name → its selected/uploaded reference images.
  const [speciesReferenceImages, setSpeciesReferenceImages] = useState<Record<string, SpeciesRefImage[]>>({});
  const [selectedRecipients, setSelectedRecipients] = useState<string[]>([]);
  const [sharedWithResearchers, setSharedWithResearchers] = useState<string[]>([]);
  const [selectedExperiments, setSelectedExperiments] = useState<string[]>([]);
  const [availableOptions, setAvailableOptions] = useState<{ id: string; label: string }[]>([]);
  const [availableResearchers, setAvailableResearchers] = useState<{ id: string; label: string }[]>([]);
  const [availableExperiments, setAvailableExperiments] = useState<{ id: string; label: string }[]>([]);
  const [availableSpecies, setAvailableSpecies] = useState<{ id: string; label: string }[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isPublic, setIsPublic] = useState(false);
  const [consensusThreshold, setConsensusThreshold] = useState<number>(3);
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];

  // Fetch the reference-image pool for the currently-selected species (by name).
  const { data: poolImagesRaw, isLoading: poolImagesLoading } = useSpeciesPoolImages(targetSpecies);
  const poolImages = poolImagesRaw ?? {};

  const handleImagesChange = (speciesName: string, images: SpeciesRefImage[]) => {
    setSpeciesReferenceImages((prev) => ({ ...prev, [speciesName]: images }));
  };

  useEffect(() => {
    const fetchOptions = async () => {
      setOptionsLoading(true);
      try {
        const [groupsRes, usersRes, experimentsRes, speciesRes, researchersRes] = await Promise.all([
          apiFetch(API_ENDPOINTS.researcher.RECIPIENTS),
          apiFetch(API_ENDPOINTS.USERS.GET_ALL),
          apiFetch(API_ENDPOINTS.TASKS.EXPERIMENTS),
          apiFetch('/api/v1/metadata/species'),
          apiFetch('/api/v1/users/roles/RESEARCHER')
        ]);

        let loaded: { id: string, label: string }[] = [];
        if (groupsRes.ok) {
          const groups = await groupsRes.json();
          groups.forEach((g: any) => loaded.push({ id: `G-${g.groupId || g.id}`, label: `(Group) ${g.name}` }));
        }
        if (usersRes.ok) {
          const users = await usersRes.json();
          users.forEach((u: any) => loaded.push({ id: `U-${u.username}`, label: `(User) ${u.username}` }));
        }
        if (experimentsRes.ok) {
          const exps = await experimentsRes.json();
          setAvailableExperiments(exps.map((e: any) => ({ id: String(e.id), label: e.name || `Experiment ${e.id}` })));
        }
        if (speciesRes.ok) {
          const sps = await speciesRes.json();
          setAvailableSpecies(sps.map((s: any) => ({ id: String(s.id), label: String(s.label) })));
        }
        if (researchersRes.ok) {
          const researchers = await researchersRes.json();
          setAvailableResearchers(researchers.map((r: any) => ({ id: r.username, label: r.displayName || r.username })));
        }
        setAvailableOptions(loaded);
      } catch (error) {
        console.error("Failed to load options:", error);
      } finally {
        setOptionsLoading(false);
      }
    };
    fetchOptions();
  }, []);

  // 🔹 Load task details
  useEffect(() => {
    apiFetch(API_ENDPOINTS.TASKS.DASHBOARD_DETAILS(taskId))
      .then(async (res) => { if (!res.ok) { let t = await res.text(); console.error("API Error", res.status, t); throw new Error(t); } return res.json(); })
      .then((data) => {
        console.log("EditTask Data Payload:", data); data = data.task || data;
        setName(data.name);
        setDescription(data.description);

        setTargetSpecies(
          data.targetSpecies
            ?.map((s: any) => String(s.name)) || []
        );

        // Seed the per-species reference images from the task's existing pool selections.
        // The backend returns imageUrl as "/api/v1/species/reference-images/{id}/image";
        // parse the pool id from it so the images show as pre-selected and round-trip on save.
        const seeded: Record<string, SpeciesRefImage[]> = {};
        (data.targetSpecies || []).forEach((s: any) => {
          const imgs = (s.referenceImages || [])
            .map((img: any): SpeciesRefImage | null => {
              const match = typeof img.imageUrl === "string"
                ? img.imageUrl.match(/reference-images\/(\d+)\//)
                : null;
              if (!match) return null;
              const poolId = Number(match[1]);
              return {
                poolId,
                uri: API_ENDPOINTS.SPECIES.REF_THUMB_URL(poolId),
                fromPool: true,
                caption: img.caption,
              };
            })
            .filter(Boolean) as SpeciesRefImage[];
          if (imgs.length > 0) seeded[String(s.name)] = imgs;
        });
        setSpeciesReferenceImages(seeded);

        setSelectedExperiments(data.experiments?.map((id: number) => String(id)) || []);

        setIsPublic(data.isPublic || false);
        setConsensusThreshold(data.consensusThreshold || 3);

        const loadedGroups = data.recipientGroups?.map((id: number) => `G-${id}`) || [];
        const loadedUsers = data.assignedUsernames?.map((un: string) => `U-${un}`) || [];
        setSelectedRecipients([...loadedGroups, ...loadedUsers]);
        setSharedWithResearchers(data.sharedWithResearchers || []);
      })
      .catch(() => {
        Alert.alert("Error", "Failed to load task data");
      });
  }, [taskId]);

  const handleSubmit = async () => {
    Keyboard.dismiss();
    if (Platform.OS === 'web' && document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }

    if (!name || !description) {
      Alert.alert("Validation Error", "Task name and description are required");
      return;
    }

    try {
      setLoading(true);

      // Resolve each species' reference images to pool image IDs. Uploads now happen
      // immediately in SpeciesImagePicker, so entries are normally fromPool already;
      // any local straggler is uploaded here (mirrors the create-task flow).
      const speciesReferenceImageIds: Record<string, number[]> = {};
      await Promise.all(
        targetSpecies.map(async (speciesName) => {
          const imgs = speciesReferenceImages[speciesName] ?? [];
          const poolIds: number[] = [];
          for (const img of imgs) {
            if (img.fromPool && img.poolId != null) {
              poolIds.push(img.poolId);
            } else {
              const fd = new FormData();
              const file = (img as any)._file as File | undefined;
              if (file) {
                fd.append("files", file);
              } else {
                fd.append("files", { uri: img.uri, name: "ref.jpg", type: "image/jpeg" } as any);
              }
              if (img.caption) fd.append("caption", img.caption);
              const res = await apiFetch(API_ENDPOINTS.SPECIES.REF_IMAGES(speciesName), {
                method: "POST",
                body: fd,
              });
              if (res.ok) {
                const saved: { id: number }[] = await res.json();
                saved.forEach((s) => poolIds.push(s.id));
              }
            }
          }
          speciesReferenceImageIds[speciesName] = poolIds;
        })
      );

      const payload = {
        status: "ACTIVE",
        name,
        description,
        experiments: selectedExperiments.map(Number),
        targetSpecies: targetSpecies.map((s) => ({ name: s })),
        speciesReferenceImageIds,
        isPublic,
        recipientGroups: selectedRecipients.filter(id => id.startsWith("G-")).map(id => Number(id.replace("G-", ""))),
        assignedUsernames: selectedRecipients.filter(id => id.startsWith("U-")).map(id => id.replace("U-", "")),
        sharedWithResearchers,
        consensusThreshold,
      };

      const res = await apiFetch(
        API_ENDPOINTS.TASKS.UPDATE_TASK(taskId),
        {
          method: "PUT",
          body: JSON.stringify(payload),
          headers: { "Content-Type": "application/json" },
        }
      );

      if (!res.ok) {
        const txt = await res.text();
        console.error("Update failed HTTP", res.status, txt);
        throw new Error("Failed to update: " + res.status);
      }

      await res.json();

      // Force immediate refetches (not lazy invalidation) so the task details
      // page reflects the new reference images right away instead of waiting out
      // the query's staleTime. The details/list queries all live under ['tasks'].
      // Await the details refetch so the data is fresh before we navigate back.
      await queryClient.invalidateQueries({ queryKey: QUERY_KEYS.taskDetails(taskId) });
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      queryClient.invalidateQueries({ queryKey: ["species", "pool"] });

      Alert.alert("Success", "Task updated successfully");
      navigation.navigate("TasksManagement");
    } catch (err) {
      console.error("Update task error:", err);
      Alert.alert("Error", "Failed to update task");
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScreenHeaderLayout
      leftIcon={require("../../../assets/images/tasks_mgmt.png")}
      leftTitle="Edit Task"
      rightIcon={require("../../../assets/images/tasks_mgmt.png")}
      rightTitle="Tasks"
      onRightPress={() => navigation.navigate("TasksManagement")}
    >
      <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
        <Text style={styles.label}>Task Name</Text>
        <TextInput
          style={styles.input}
          value={name}
          onChangeText={setName}
        />

        <Text style={[styles.label, { color: themeColors.text }]}>Description</Text>
        <TextInput
          style={[styles.input, { backgroundColor: themeColors.background, borderColor: themeColors.border, color: themeColors.text }]}
          value={description}
          onChangeText={setDescription}
          placeholderTextColor={themeColors.textSecondary}
        />

        <Text style={[styles.label, { color: themeColors.text }]}>
          Target Species
        </Text>
        <MultiSelect
          options={availableSpecies}
          selectedIds={targetSpecies}
          onToggle={(id) => {
            const sid = id as string;
            setTargetSpecies((prev) =>
              prev.includes(sid) ? prev.filter((s) => s !== sid) : [...prev, sid]
            );
            // Drop reference images for a species that was just deselected.
            setSpeciesReferenceImages((prev) => {
              if (!prev[sid]) return prev;
              const { [sid]: _removed, ...rest } = prev;
              return rest;
            });
          }}
          placeholder="Search species..."
          loading={optionsLoading}
        />

        {/* Reference image pickers — one per selected species */}
        {targetSpecies.length > 0 && (
          <View style={styles.pickersSection}>
            <Text style={[styles.pickersHeading, { color: themeColors.textSecondary }]}>
              REFERENCE IMAGES
            </Text>

            {poolImagesLoading && (
              <View style={styles.poolLoadingRow}>
                <ActivityIndicator size="small" color="#10B981" />
                <Text style={[styles.poolLoadingText, { color: themeColors.textSecondary }]}>
                  Loading pool images...
                </Text>
              </View>
            )}

            {targetSpecies.map((speciesName) => (
              <SpeciesImagePicker
                key={speciesName}
                speciesId={speciesName}
                speciesLabel={speciesName}
                selectedImages={speciesReferenceImages[speciesName] ?? []}
                poolImages={poolImages[speciesName] ?? []}
                poolLoading={poolImagesLoading}
                onImagesChange={handleImagesChange}
              />
            ))}
          </View>
        )}

        <View style={{ marginTop: 16, marginBottom: 16 }}>
          <Text style={[styles.pickersHeading, { color: themeColors.textSecondary }]}>
            CONSENSUS SETTINGS
          </Text>
          <View style={[styles.consensusContainer, { backgroundColor: themeColors.card, borderColor: themeColors.border }]}>
            <View style={styles.consensusTextWrapper}>
              <Text style={[styles.consensusLabel, { color: themeColors.text }]}>Threshold</Text>
              <Text style={[styles.consensusHint, { color: themeColors.textSecondary }]}>
                Cumulative score required for an image to reach consensus (3 - 20). 
                E.g., 3 requires 3 expert classifications.
              </Text>
            </View>
            <View style={styles.stepperContainer}>
              <TouchableOpacity
                style={[styles.stepperBtn, { borderColor: themeColors.border, backgroundColor: themeColors.background }]}
                onPress={() => {
                  const val = Math.max(3, consensusThreshold - 1);
                  setConsensusThreshold(val);
                }}
                disabled={consensusThreshold <= 3}
              >
                <Text style={[styles.stepperBtnText, { color: consensusThreshold <= 3 ? themeColors.textSecondary : themeColors.text }]}>-</Text>
              </TouchableOpacity>
              <Text style={[styles.stepperValue, { color: themeColors.text }]}>{consensusThreshold}</Text>
              <TouchableOpacity
                style={[styles.stepperBtn, { borderColor: themeColors.border, backgroundColor: themeColors.background }]}
                onPress={() => {
                  const val = Math.min(20, consensusThreshold + 1);
                  setConsensusThreshold(val);
                }}
                disabled={consensusThreshold >= 20}
              >
                <Text style={[styles.stepperBtnText, { color: consensusThreshold >= 20 ? themeColors.textSecondary : themeColors.text }]}>+</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>

        <Text style={[styles.label, { color: themeColors.text }]}>Experiments</Text>
        <MultiSelect
          options={availableExperiments}
          selectedIds={selectedExperiments || []}
          onToggle={(id) => {
            setSelectedExperiments((prev) =>
              (prev || []).includes(id as string) ? prev.filter((eid) => eid !== id) : [...(prev || []), id as string]
            );
          }}
          placeholder="Search experiments..."
          loading={optionsLoading}
        />

        <Text style={[styles.label, { color: themeColors.text }]}>Task Visibility</Text>
        <View style={{ flexDirection: 'row', marginBottom: 12, marginTop: 4 }}>
          <TouchableOpacity
            style={[styles.toggleBtn, isPublic && styles.toggleActive]}
            onPress={() => setIsPublic(true)}
          >
            <Text style={[styles.toggleText, isPublic && styles.toggleTextActive]}>Public (All)</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.toggleBtn, !isPublic && styles.toggleActive]}
            onPress={() => setIsPublic(false)}
          >
            <Text style={[styles.toggleText, !isPublic && styles.toggleTextActive]}>Restricted</Text>
          </TouchableOpacity>
        </View>

        {!isPublic && (
          <>
            <Text style={[styles.label, { color: themeColors.text }]}>Assign Recipients</Text>
            <MultiSelect
              options={availableOptions}
              selectedIds={selectedRecipients || []}
              onToggle={(id) => {
                setSelectedRecipients((prev) =>
                  (prev || []).includes(id as string) ? prev.filter((gid) => gid !== id) : [...(prev || []), id as string]
                );
              }}
              placeholder="Search users or groups..."
              loading={optionsLoading}
            />
          </>
        )}

        <Text style={[styles.label, { color: themeColors.text }]}>Share with Co-Managers</Text>
        <MultiSelect
          options={availableResearchers}
          selectedIds={sharedWithResearchers || []}
          onToggle={(id) => {
            setSharedWithResearchers((prev) =>
              (prev || []).includes(id as string) ? prev.filter((rid) => rid !== id) : [...(prev || []), id as string]
            );
          }}
          placeholder="Search researchers..."
          loading={optionsLoading}
        />

        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleSubmit}
          disabled={loading}
        >
          <Text style={styles.buttonText}>
            {loading ? "Updating..." : "Update Task"}
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </ScreenHeaderLayout>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  label: {
    fontWeight: "600",
    marginTop: 12,
    marginBottom: 4,
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 10,
    padding: 10,
    backgroundColor: "#fff",
  },
  button: {
    marginTop: 24,
    backgroundColor: "#2563EB",
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
  },
  buttonDisabled: {
    backgroundColor: "#93C5FD",
  },
  buttonText: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 16,
  },
  toggleBtn: {
    flex: 1,
    paddingVertical: 10,
    backgroundColor: "#f3f4f6",
    alignItems: "center",
    borderRadius: 8,
    marginHorizontal: 4,
    borderWidth: 1,
    borderColor: "#e5e7eb",
  },
  toggleActive: {
    backgroundColor: "#d1fae5",
    borderColor: "#10B981",
  },
  toggleText: {
    fontWeight: "600",
    color: "#6b7280",
  },
  toggleTextActive: {
    color: "#065f46",
  },
  pickersSection: { marginTop: 8, gap: 4 },
  pickersHeading: { fontSize: 11, fontWeight: "700", letterSpacing: 0.8, marginBottom: 8, marginTop: 12 },
  poolLoadingRow: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 12 },
  poolLoadingText: { fontSize: 13 },
  consensusContainer:{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16, borderRadius: 12, borderWidth: 1, gap: 16 },
  consensusTextWrapper: { flex: 1 },
  consensusLabel:    { fontSize: 16, fontWeight: '600', marginBottom: 4 },
  consensusHint:     { fontSize: 13, lineHeight: 18 },
  stepperContainer:  { flexDirection: 'row', alignItems: 'center', gap: 12 },
  stepperBtn:        { width: 36, height: 36, borderRadius: 18, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  stepperBtnText:    { fontSize: 18, fontWeight: '700' },
  stepperValue:      { fontSize: 18, fontWeight: '700', minWidth: 24, textAlign: 'center' },
});
