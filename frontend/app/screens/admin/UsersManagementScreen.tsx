import React from 'react';
import { StyleSheet, Text, View, Image } from 'react-native';

export default function UsersManagementScreen() {
    return (
        <View style={styles.container}>
            <View style={styles.content}>
                <Image
                    source={require('../../../assets/images/users.png')}
                    style={styles.icon}
                />
                <Text style={styles.title}>Users Management</Text>
                <Text style={styles.subtitle}>Coming Soon</Text>
                <Text style={styles.description}>
                    This section will allow you to manage individual user accounts, permissions, and roles.
                </Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
        justifyContent: 'center',
        alignItems: 'center',
    },
    content: {
        alignItems: 'center',
        padding: 30,
    },
    icon: {
        width: 80,
        height: 80,
        marginBottom: 20,
        tintColor: '#ccc'
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#333',
        marginBottom: 10,
    },
    subtitle: {
        fontSize: 18,
        color: '#4B7BE5',
        marginBottom: 20,
        fontWeight: '600'
    },
    description: {
        textAlign: 'center',
        color: '#666',
        lineHeight: 22,
    }
});
