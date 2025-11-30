import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const SwipeScreen = () => {
    // TODO: Fetch images batch
    // TODO: Implement Swipe logic (Yes/No/Skip)
    return (
        <View style={styles.container}>
            <Text>Swipe Screen</Text>
            {/* TODO: Render SwipeCard component */}
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

export default SwipeScreen;
