import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import { useNavigation } from '@react-navigation/native';

import TaskCard from '../../components/user/TaskCard';

// Mocks
import { statisticsMock } from '../../mocks/data/statistics.mock';
import { dashboardUserMock } from '../../mocks/data/dashboard.user.mock';
import { authMock } from '../../mocks/data/auth.mock'; // For logout or user info if needed

export default function UserMyTasksScreen() {
    const navigation = useNavigation<any>();
    const [refreshing, setRefreshing] = useState(false);

    // State for data
    const [stats, setStats] = useState(statisticsMock.summary);
    const [tasks, setTasks] = useState(dashboardUserMock.tasks.tasks);
    const [availableTasks, setAvailableTasks] = useState(dashboardUserMock.availableTasks || []);

    // Simulate fetching data
    const loadData = useCallback(() => {
        setRefreshing(true);
        setTimeout(() => {
            // In a real app, you would fetch fresh data here
            // For now, we just reset from mocks to simulate a refresh
            setStats(statisticsMock.summary);
            // setTasks(dashboardUserMock.tasks.tasks); // Don't reset if we want to keep added tasks for demo
            setAvailableTasks(dashboardUserMock.availableTasks || []);
            setRefreshing(false);
        }, 1000);
    }, []);

    const handleAddTask = (taskId: number) => {
        // Move from available to my tasks
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
        // Optionally add it back to availableTasks?
        // const removedTask = tasks.find(t => t.taskId === taskId);
        // if(removedTask) setAvailableTasks(prev => [...prev, removedTask]);
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

    const handlePlayTask = (taskId: number) => {
        console.log(`Playing task ${taskId}`);
        // Navigate to the swipe screen (gameplay)
        navigation.navigate('SwipeLab');
    };

    return (
        <View style={styles.container}>
            <ScrollView
                contentContainerStyle={styles.scrollContent}
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={loadData} />
                }
            >
                {/* ... existing content ... */}
                {/* Section Title */}
                <View style={styles.sectionHeader}>
                    <View style={styles.titleBlock}>
                        {/* Icon could go here */}
                        <Text style={styles.sectionTitle}>My Tasks</Text>
                    </View>

                    {/* Completion Dial Placeholder - simple text for now as per design mockup it's a visual element */}
                    <View style={styles.completionBlock}>
                        <Text style={styles.completionText}>Completion</Text>
                        {/* Could be a circular progress component */}
                    </View>
                </View>

                {tasks.length === 0 && (
                    <Text style={styles.emptyState}>No tasks assigned yet.</Text>
                )}

                {tasks.map((task) => {
                    // Calculate progress
                    const progress = Math.round((task.imagesClassified / task.totalImages) * 100);

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={task.species.map(s => s.name)}
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
                    const progress = Math.round((task.imagesClassified / task.totalImages) * 100);

                    return (
                        <TaskCard
                            key={task.taskId}
                            title={task.name}
                            description={task.description}
                            species={task.species.map(s => s.name)}
                            imagesClassified={task.imagesClassified}
                            progress={progress}
                            onPlay={() => handleAddTask(task.taskId)}
                            onPress={() => handleTaskPress(task)}
                            actionType="add"
                        />
                    );
                })}
                {availableTasks.length === 0 && (
                    <Text style={styles.emptyState}>No new tasks available.</Text>
                )}

            </ScrollView>
        </View>
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
