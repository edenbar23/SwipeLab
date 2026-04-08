import React, { useEffect, useState } from "react";
import { Alert, KeyboardAvoidingView, Platform, StyleSheet, View } from "react-native";
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
import StepName from "../../components/admin/addTask/StepName";
import StepRecipients from "../../components/admin/addTask/StepRecipients";
import StepSpecies from "../../components/admin/addTask/StepSpecies";

const STEPS = ["Name", "Description", "Species", "Recipients", "Confirm"];

export default function AddTaskScreen({ navigation }: any) {
  const { theme } = useThemeStore();
  const themeColors = Colors[theme as keyof typeof Colors];
  const queryClient = useQueryClient();

  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState<AddTaskFormData>({
    name: "",
    description: "",
    speciesList: [],
    isPublic: false,
    selectedRecipients: [],
  });

  const [availableOptions, setAvailableOptions] = useState<{ id: string; label: string }[]>([]);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchOptions = async () => {
      setOptionsLoading(true);
      try {
        const [groupsRes, usersRes] = await Promise.all([
          apiFetch(API_ENDPOINTS.ADMIN.RECIPIENTS),
          apiFetch(API_ENDPOINTS.USERS.GET_ALL)
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
      targetSpecies: formData.speciesList.map((s) => ({ commonName: s.trim() })),
      isPublic: formData.isPublic,
      recipientGroups: formData.selectedRecipients.filter(id => id.startsWith("G-")).map(id => Number(id.replace("G-", ""))),
      assignedUsernames: formData.selectedRecipients.filter(id => id.startsWith("U-")).map(id => id.replace("U-", "")),
    };

    try {
      setLoading(true);
      const res = await apiFetch("/api/v1/dashboard/tasks", {
        method: "POST",
        body: JSON.stringify(payload),
        headers: { "Content-Type": "application/json" },
      });

      if (!res.ok) {
        throw new Error("Failed response");
      }

      queryClient.invalidateQueries({ queryKey: ["tasks"] });

      Alert.alert("Success", "Task created successfully!");
      navigation.navigate("Tasks Management"); // go back to tasks list
    } catch (err) {
      console.error("Error creating task:", err);
      Alert.alert("Error", "Failed to create task");
    } finally {
      setLoading(false);
    }
  };

  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return <StepName formData={formData} onUpdate={updateFormData} onNext={nextStep} />;
      case 1:
        return <StepDescription formData={formData} onUpdate={updateFormData} onNext={nextStep} onBack={prevStep} />;
      case 2:
        return <StepSpecies formData={formData} onUpdate={updateFormData} onNext={nextStep} onBack={prevStep} />;
      case 3:
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
      case 4:
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
        <StepIndicator steps={STEPS} currentStep={currentStep} />

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
});
