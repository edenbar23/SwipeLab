import React from 'react';
import { View, StyleSheet } from 'react-native';

const ProgressBar = ({ progress }) => {
    return (
        <View style={styles.container}>
            {/* TODO: Implement progress bar logic */}
            <View style={[styles.bar, { width: `${progress}%` }]} />
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        // TODO: Add container styling
    },
    bar: {
        // TODO: Add bar styling
    },
});

export default ProgressBar;
