import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const LoginScreen = () => {
  // TODO: Implement Google Sign-In logic
  return (
    <View style={styles.container}>
      <Text>Login Screen</Text>
      {/* TODO: Add Google Sign-In Button */}
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

export default LoginScreen;
