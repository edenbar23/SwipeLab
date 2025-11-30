import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const LeaderboardScreen = () => {
    // TODO: Fetch leaderboard data
    return (
        <View style={styles.container}>
            <Text>Leaderboard Screen</Text>
            {/* TODO: Render list of top users */}
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

export default LeaderboardScreen;
