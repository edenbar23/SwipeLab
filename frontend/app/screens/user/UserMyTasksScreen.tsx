import { useNavigation } from '@react-navigation/native';
import React, { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Colors } from '../../../constants/theme';
import { apiFetch } from '../../api/apiFetch';
import ScreenHeaderLayout from '../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout';
import TaskCard from '../../components/user/TaskCard';
import { useThemeStore } from '../../stores/themeStore';

export default function UserMyTasksScreen() {
    const navigation = useNavigation<any>();
    const { theme } = useThemeStore();
    const themeColors = Colors[theme as keyof typeof Colors];
    const [refreshing, setRefreshing] = useState(false);

    // State for data
    const [stats, setStats] = useState<any>(null);
    const [tasks, setTasks] = useState<any[]>([]);
    const [availableTasks, setAvailableTasks] = useState<any[]>([]);

    const loadData = useCallback(async () => {
        setRefreshing(true);
        try {
            const [statsRes, tasksRes, availableRes] = await Promise.all([
                apiFetch('/api/v1/statistics/me').catch(() => null),
                apiFetch('/api/v1/tasks/my-tasks').catch(() => null),
                apiFetch('/api/v1/tasks/available-tasks').catch(() => null)
            ]);

            if (statsRes && statsRes.ok) setStats(await statsRes.json());
            if (tasksRes && tasksRes.ok) {
                const tasksData = await tasksRes.json();
                setTasks(tasksData.tasks || []);
            }

            if (availableRes && availableRes.ok) {
                setAvailableTasks(await availableRes.json());
            } else {
                console.log('available tasks not implemented yet');
            }
        } catch (e) {
            console.error('Failed to load tasks data:', e);
        } finally {
            setRefreshing(false);
        }
    }, []);

    const handleAddTask = (taskId: number) => {
        const taskToAdd = availableTasks.find(t => t.taskId === taskId);
        if (taskToAdd) {
            setTasks(prev => [...prev, taskToAdd]);
            setAvailableTasks(prev => prev.filter(t => t.taskId !== taskId));
            console.log(`Added task ${taskId} to my tasks`);
        }
    };

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handleTaskPress = (task: any) => {
        navigation.navigate('TaskDetails', { task });
    };

    const handleRemoveTask = (taskId: number) => {
        setTasks(prev => prev.filter(t => t.taskId !== taskId));
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
    tasks.forEach(t => {
        totalImages += t.totalImages;
        totalClassified += t.imagesClassified;
    });
    const completionPercent = totalImages > 0 ? Math.round((totalClassified / totalImages) * 100) : 0;

    const handlePlayTask = (taskId: number) => {
        console.log(`Playing task ${taskId}`);
        // Navigate to the swipe screen (gameplay)
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

                {tasks.map((task) => {
                    // Calculate progress
                    const progress = task.totalImages ? Math.round((task.imagesClassified / task.totalImages) * 100) : 0;

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={task.species ? task.species.map((s: any) => s.name) : []}
                            imagesClassified={task.imagesClassified}
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

                {availableTasks.map((task) => {
                    // Calculate progress
                    const progress = task.totalImages ? Math.round((task.imagesClassified / task.totalImages) * 100) : 0;

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={task.species ? task.species.map((s: any) => s.name) : []}
                            imagesClassified={task.imagesClassified}
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
