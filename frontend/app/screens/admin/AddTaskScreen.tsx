import React, { useEffect, useState } from "react";
import { Alert, KeyboardAvoidingView, Platform, StyleSheet, View, Text, TouchableOpacity } from "react-native";
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from "../../api/apiEndpoints";
import { apiFetch } from "../../api/apiFetch";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout";
import { useQueryClient } from "@tanstack/react-query";
import StepIndicator from "../../components/ui/StepIndicator";
import { useThemeStore } from '../../stores/themeStore';

import { AddTaskFormData } from "../../components/admin/addTask/addTaskTypes";
import StepConfirm from "../../components/admin/addTask/StepConfirm";
import StepDescription from "../../components/admin/addTask/StepDescription";
import { useAuthStore } from "../../stores/authStore";
import StepName from "../../components/admin/addTask/StepName";
import StepRecipients from "../../components/admin/addTask/StepRecipients";
import StepSpecies from "../../components/admin/addTask/StepSpecies";
import StepExperiments from "../../components/admin/addTask/StepExperiments";

const STEPS = ["Name", "Description", "Experiments", "Species", "Recipients", "Confirm"];

export default function AddTaskScreen({ route, navigation }: any) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const queryClient = useQueryClient();

  const [currentStep, setCurrentStep] = useState(0);
  const [createdTaskId, setCreatedTaskId] = useState<string | null>(null);
  const [formData, setFormData] = useState<AddTaskFormData>({
    name: "",
    description: "",
    selectedExperiments: [],
    speciesList: route?.params?.initialSpecies || [],
    isPublic: false,
    selectedRecipients: [],
  });

  const [availableOptions, setAvailableOptions] = useState<{ id: string; label: string }[]>([]);
  const [availableExperiments, setAvailableExperiments] = useState<{ id: string; label: string }[]>([]);
  const [availableSpecies, setAvailableSpecies] = useState<{ id: string; label: string }[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchOptions = async () => {
      setOptionsLoading(true);
      try {
        const [groupsRes, usersRes, experimentsRes, speciesRes] = await Promise.all([
          apiFetch(API_ENDPOINTS.ADMIN.RECIPIENTS),
          apiFetch(API_ENDPOINTS.USERS.GET_ALL),
          apiFetch(API_ENDPOINTS.TASKS.EXPERIMENTS),
          apiFetch('/api/v1/metadata/species')
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
          setAvailableSpecies(sps.map((s: any) => ({ 
            id: String(s.id), 
            label: String(s.label),
            searchTerms: String(s.searchTerms || "") 
          })));
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

  const updateFormData = (updates: Partial<AddTaskFormData>) => {
    setFormData((prev) => ({ ...prev, ...updates }));
  };

  const nextStep = () => {
    if (currentStep < STEPS.length - 1) {
      setCurrentStep((prev) => prev + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const handleSubmit = async () => {
    if (!formData.name || !formData.description) {
      Alert.alert("Validation Error", "Task name and description are required");
      return;
    }

    const payload = {
      name: formData.name,
      description: formData.description,
      experiments: formData.selectedExperiments.map(Number),
      targetSpecies: formData.speciesList.map((s) => ({ name: s })),
      isPublic: formData.isPublic,
      recipientGroups: formData.selectedRecipients.filter(id => id.startsWith("G-")).map(id => Number(id.replace("G-", ""))),
      assignedUsernames: formData.selectedRecipients.filter(id => id.startsWith("U-")).map(id => id.replace("U-", "")),
    };

    try {
      setLoading(true);

      const authState = useAuthStore.getState();
      const isStardbi = authState.authProvider === "STARDBI";
      let headers: any = { "Content-Type": "application/json" };
      
      if (isStardbi && authState.token) {
        headers["X-Stardbi-Access-Token"] = authState.token;
        if (Platform.OS === 'web') {
          headers["X-Stardbi-Refresh-Token"] = localStorage.getItem("refreshToken") || "";
        }
        // In React Native environment, getting SecureStore synchronously is tricky,
        // but typically refreshToken is stored securely. For web, localStorage is fast.
      }

      const res = await apiFetch(API_ENDPOINTS.TASKS.CREATE_TASK, {
        method: "POST",
        body: JSON.stringify(payload),
        headers,
      });

      if (!res.ok) {
        throw new Error("Failed response");
      }

      const resData = await res.json();
      queryClient.invalidateQueries({ queryKey: ["tasks"] });

      setCreatedTaskId(resData.taskId);
    } catch (err) {
      console.error("Error creating task:", err);
      Alert.alert("Error", "Failed to create task");
    } finally {
      setLoading(false);
    }
  };

  const renderStepContent = () => {
    if (createdTaskId) {
      return (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', gap: 16 }}>
          <Ionicons name="checkmark-circle" size={80} color="#10B981" />
          <Text style={{ fontSize: 24, fontWeight: 'bold', color: themeColors.text }}>Task Created!</Text>
          <Text style={{ fontSize: 16, color: themeColors.text, opacity: 0.8, textAlign: 'center' }}>
            Your task has been successfully added to the system.
          </Text>
          <View style={{ flexDirection: 'row', gap: 12, marginTop: 24 }}>
            <TouchableOpacity 
              style={{ paddingHorizontal: 20, paddingVertical: 12, backgroundColor: themeColors.card, borderRadius: 8, borderWidth: 1, borderColor: themeColors.border }}
              onPress={() => {
                setCreatedTaskId(null);
                setCurrentStep(0);
                setFormData({
                  name: "",
                  description: "",
                  selectedExperiments: [],
                  speciesList: [],
                  isPublic: false,
                  selectedRecipients: [],
                });
              }}
            >
              <Text style={{ color: themeColors.text, fontWeight: '600' }}>Create Another</Text>
            </TouchableOpacity>
            <TouchableOpacity 
              style={{ paddingHorizontal: 20, paddingVertical: 12, backgroundColor: themeColors.tint, borderRadius: 8 }}
              onPress={() => navigation.navigate("TasksManagement")}
            >
              <Text style={{ color: '#fff', fontWeight: 'bold' }}>Go to Tasks</Text>
            </TouchableOpacity>
          </View>
        </View>
      );
    }

    switch (currentStep) {
      case 0:
        return <StepName formData={formData} onUpdate={updateFormData} onNext={nextStep} />;
      case 1:
        return <StepDescription formData={formData} onUpdate={updateFormData} onNext={nextStep} onBack={prevStep} />;
      case 2:
        return (
          <StepExperiments
            formData={formData}
            onUpdate={updateFormData}
            onNext={nextStep}
            onBack={prevStep}
            availableExperiments={availableExperiments}
            optionsLoading={optionsLoading}
          />
        );
      case 3:
        return (
          <StepSpecies
            formData={formData}
            onUpdate={updateFormData}
            onNext={nextStep}
            onBack={prevStep}
            availableSpecies={availableSpecies}
            optionsLoading={optionsLoading}
          />
        );
      case 4:
        return (
          <StepRecipients
            formData={formData}
            onUpdate={updateFormData}
            onNext={nextStep}
            onBack={prevStep}
            availableOptions={availableOptions}
            optionsLoading={optionsLoading}
          />
        );
      case 5:
        return (
          <StepConfirm
            formData={formData}
            onUpdate={updateFormData}
            onNext={nextStep}
            onBack={prevStep}
            onSubmit={handleSubmit}
            loading={loading}
            availableOptions={availableOptions}
          />
        );
      default:
        return null;
    }
  };

  return (
    <ScreenHeaderLayout
      leftIcon={require("../../../assets/images/add_task.png")}
      leftTitle="Add Task"
      rightIcon={require("../../../assets/images/tasks_mgmt.png")}
      rightTitle="Tasks"
      onRightPress={() => navigation.navigate("TasksManagement")}
    >
      <KeyboardAvoidingView
        style={[styles.container, { backgroundColor: themeColors.background }]}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {/* Step Progress Indicator */}
        {!createdTaskId && <StepIndicator steps={STEPS} currentStep={currentStep} />}

        {/* Selected Species Banner */}
        {formData.speciesList.length > 0 && currentStep < 3 && (
          <View style={[styles.banner, { backgroundColor: themeColors.card, borderColor: '#10B981' }]}>
            <Ionicons name="leaf" size={20} color="#10B981" />
            <Text style={[styles.bannerText, { color: themeColors.text }]}>
              {formData.speciesList.length} species selected for this task
            </Text>
          </View>
        )}

        {/* Current Step Content */}
        <View style={styles.contentContainer}>
          {renderStepContent()}
        </View>
      </KeyboardAvoidingView>
    </ScreenHeaderLayout>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  contentContainer: {
    flex: 1,
    paddingHorizontal: 16,
    paddingBottom: 16,
  },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 16,
    marginBottom: 16,
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    gap: 8,
  },
  bannerText: {
    fontSize: 14,
    fontWeight: '600',
  },
});
