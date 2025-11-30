import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const HomeScreen = () => {
    // TODO: Fetch user stats and progress
    return (
        <View style={styles.container}>
            <Text>Home Screen</Text>
            {/* TODO: Add navigation to Swipe, Profile, Leaderboard */}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
});

export default HomeScreen;
