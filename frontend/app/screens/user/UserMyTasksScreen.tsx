import { useNavigation } from '@react-navigation/native';
import React, { useCallback, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useMyTasks, useAvailableTasks, useStatistics, QUERY_KEYS } from "../../api/queries";
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { API_ENDPOINTS } from '../../api/apiEndpoints';
import { apiFetch } from '../../api/apiFetch';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import TaskCard from '../../components/user/TaskCard';
import { useThemeStore } from '../../stores/themeStore';
import { useSwipeStore } from '../../stores/swipeStore';


export default function UserMyTasksScreen() {
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const queryClient = useQueryClient();
    const { setActiveTaskId } = useSwipeStore();

    const { data: stats, isLoading: statsLoading, refetch: refetchStats } = useStatistics();
    const { data: tasks = [], isLoading: tasksLoading, refetch: refetchTasks } = useMyTasks();
    const { data: availableTasks = [], isLoading: availableLoading, refetch: refetchAvailable } = useAvailableTasks();

    const refreshing = statsLoading || tasksLoading || availableLoading;

    const loadData = useCallback(() => {
        refetchStats();
        refetchTasks();
        refetchAvailable();
    }, [refetchStats, refetchTasks, refetchAvailable]);

    const handleAddTask = (taskId: number) => {
        const taskToAdd = availableTasks.find((t: any) => t.taskId === taskId);
        if (taskToAdd) {
            queryClient.setQueryData(QUERY_KEYS.myTasks, (prev: any) => [...(prev || []), taskToAdd]);
            queryClient.setQueryData(QUERY_KEYS.availableTasks, (prev: any) => 
                (prev || []).filter((t: any) => t.taskId !== taskId)
            );
            console.log(`Added task ${taskId} to my tasks`);
        }
    };

    const handleTaskPress = (task: any) => {
        navigation.navigate('TaskDetails', { task });
    };

    const handleRemoveTask = (taskId: number) => {
        queryClient.setQueryData(QUERY_KEYS.myTasks, (prev: any) => 
            (prev || []).filter((t: any) => t.taskId !== taskId)
        );
    };

    const handleLogout = () => {
        // Navigate back to auth or reset stack
        // For mock purposes:
        console.log("Logout pressed");
        // navigation.replace('Auth'); // If you have an Auth stack
        // Or just go back if it's within a stack
        if (navigation.canGoBack()) {
            navigation.goBack();
        }
    };

    // Calculate total completion
    const totalAssigned = tasks.length;
    let totalImages = 0;
    let totalClassified = 0;
    tasks.forEach((t: any) => {
        totalImages += t.progress?.totalImages ?? t.totalImages ?? 0;
        totalClassified += t.progress?.imagesClassified ?? t.imagesClassified ?? 0;
    });
    const completionPercent = totalImages > 0 ? Math.round((totalClassified / totalImages) * 100) : 0;

    const handlePlayTask = (taskId: number) => {
        // Persist chosen task in global store, then navigate to the Swipe tab
        setActiveTaskId(taskId);
        navigation.navigate('SwipeLab');
    };

    return (
        <ScreenHeaderLayout
            leftIcon={require('../../../assets/images/tasks.png')}
            leftTitle="My Tasks"
            rightIcon={require('../../../assets/images/stats.png')}
            rightTitle={`Completion: ${completionPercent}%`}
            contentContainerStyle={{ padding: 0 }}
        >
            <ScrollView
                style={{ backgroundColor: themeColors.background }}
                contentContainerStyle={[styles.scrollContent, { backgroundColor: themeColors.background }]}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={loadData} tintColor={themeColors.text} />
                }
            >
                {/* ... existing content ... */}
                {/* Section Title */}
                <View style={[styles.sectionHeader, { marginBottom: 10 }]}>
                    <Text style={[styles.sectionTitle, { color: themeColors.text }]}>Assigned Tasks</Text>
                </View>

                {tasks.length === 0 && (
                    <Text style={[styles.emptyState, { color: themeColors.textSecondary }]}>No tasks assigned yet.</Text>
                )}

                {tasks.map((task: any) => {
                    // Calculate progress
                    const totalImages = task.progress?.totalImages ?? task.totalImages ?? 0;
                    const imagesClassified = task.progress?.imagesClassified ?? task.imagesClassified ?? 0;
                    const progress = totalImages > 0 ? Math.round((imagesClassified / totalImages) * 100) : 0;

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={Array.isArray(task.targetSpecies) ? task.targetSpecies.map((s: any) => s.name ?? s) : []}
                            imagesClassified={imagesClassified}
                            progress={progress}
                            onPlay={() => handlePlayTask(task.taskId)}
                            onPress={() => handleTaskPress(task)}
                            actionType="play"
                        />
                    );
                })}

                <View style={[styles.sectionHeader, { marginTop: 30 }]}>
                    <Text style={[styles.sectionTitle, { color: '#0EA5E9' }]}>Explore Tasks</Text>
                </View>

                {availableTasks.map((task: any) => {
                    // Calculate progress
                    const totalImages = task.progress?.totalImages ?? task.totalImages ?? 0;
                    const imagesClassified = task.progress?.imagesClassified ?? task.imagesClassified ?? 0;
                    const progress = totalImages > 0 ? Math.round((imagesClassified / totalImages) * 100) : 0;

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={Array.isArray(task.targetSpecies) ? task.targetSpecies.map((s: any) => s.name ?? s) : []}
                            imagesClassified={imagesClassified}
                            progress={progress}
                            onPlay={() => handleAddTask(task.taskId)}
                            onPress={() => handleTaskPress(task)}
                            actionType="add"
                        />
                    );
                })}
                {availableTasks.length === 0 && (
                    <Text style={[styles.emptyState, { color: themeColors.textSecondary }]}>No public tasks available.</Text>
                )}

            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
    },
    scrollContent: {
        padding: 16,
        paddingBottom: 80, // Space for bottom bar
    },
    sectionHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 20,
        marginTop: 10,
    },
    titleBlock: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    sectionTitle: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#004d40', // Dark teal
    },
    completionBlock: {
        alignItems: 'center',
    },
    completionText: {
        fontSize: 12,
        color: '#666',
    },
    emptyState: {
        textAlign: 'center',
        marginTop: 40,
        color: '#888',
        fontSize: 16,
    }
});
