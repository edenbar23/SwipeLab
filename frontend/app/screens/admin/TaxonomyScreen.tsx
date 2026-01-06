import React, { useState } from "react";
import { StyleSheet, Text, TouchableOpacity, View, ScrollView } from "react-native";
import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { AdminStackParamList } from "../../navigation/adminStack.types";
import ScreenHeaderLayout from "../../components/layout/ScreenHeaderLayout/ScreenHeaderLayout";

// Images
import taxonomyImg from "../../../assets/images/taxonomy.png";
import addTaskImg from "../../../assets/images/add_task.png";

type TaxonomyScreenNavigationProp = NativeStackNavigationProp<AdminStackParamList, 'Taxonomy'>;

const TAXONOMY_RANKS = [
    "Classes",
    "Orders",
    "Families",
    "Genus",
    "Species",
];

export default function TaxonomyScreen() {
    const navigation = useNavigation<TaxonomyScreenNavigationProp>();

    const handleRankPress = (rank: string) => {
        // For now, we just log the press. In the future, this will navigate to a detail list.
        console.log(`Open ${rank} list`);
    };

    return (
        <ScreenHeaderLayout
            leftIcon={taxonomyImg}
            leftTitle="Taxonomy"
            rightIcon={addTaskImg}
            rightTitle="Add Task"
            onRightPress={() => navigation.navigate("AddTask")}
        >
            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.listContainer}>
                    {TAXONOMY_RANKS.map((rank, index) => (
                        <TouchableOpacity
                            key={index}
                            style={styles.rankButton}
                            onPress={() => handleRankPress(rank)}
                        >
                            <Text style={styles.rankText}>{rank}</Text>
                        </TouchableOpacity>
                    ))}
                </View>
            </ScrollView>
        </ScreenHeaderLayout>
    );
}

const styles = StyleSheet.create({
    container: {
        padding: 16,
        alignItems: 'center',
    },
    listContainer: {
        width: '100%',
        maxWidth: 600, // Limit width on larger screens for better aesthetics
        gap: 16,
    },
    rankButton: {
        backgroundColor: 'white',
        paddingVertical: 16,
        paddingHorizontal: 24,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: '#e0e0e0', // Light grey border
        alignItems: 'center',
        shadowColor: "#000",
        shadowOffset: {
            width: 0,
            height: 2,
        },
        shadowOpacity: 0.1,
        shadowRadius: 3.84,
        elevation: 3,
    },
    rankText: {
        fontSize: 18,
        fontWeight: '500',
        color: '#555', // Dark grey text
    },
});
