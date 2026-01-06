import { User, usersMock } from './users.mock';

export interface RecipientGroup {
    id: number;
    name: string;
    usersCount: number;
    users: User[];
}

let recipientGroups: RecipientGroup[] = [
    {
        id: 1,
        name: 'North Cape Experts',
        usersCount: 23,
        users: [usersMock[2]] // Mock data
    },
    {
        id: 2,
        name: 'Students Bonus Group',
        usersCount: 14,
        users: [usersMock[0], usersMock[1], usersMock[7]]
    },
    {
        id: 3,
        name: 'Volunteers Group',
        usersCount: 35,
        users: [usersMock[3], usersMock[4]]
    },
    {
        id: 4,
        name: 'BGU Researchers',
        usersCount: 18,
        users: [usersMock[5], usersMock[6]]
    },
    {
        id: 5,
        name: 'Students Bonus Group',
        usersCount: 14,
        users: []
    },
    {
        id: 6,
        name: 'South Experts Group',
        usersCount: 8,
        users: []
    }
];

export const getRecipientGroups = () => recipientGroups;

export const addRecipientGroup = (name: string) => {
    const newGroup: RecipientGroup = {
        id: Math.floor(Math.random() * 10000),
        name,
        usersCount: 0,
        users: []
    };
    recipientGroups = [...recipientGroups, newGroup];
    return newGroup;
};

export const addUserToGroup = (groupId: number, userId: number) => {
    const groupIndex = recipientGroups.findIndex(g => g.id === groupId);
    if (groupIndex === -1) return null;

    const user = usersMock.find(u => u.id === userId);
    if (!user) return null;

    const group = recipientGroups[groupIndex];
    if (group.users.find(u => u.id === userId)) return group; // User already exists

    const updatedGroup = {
        ...group,
        users: [...group.users, user],
        usersCount: group.usersCount + 1
    };
    recipientGroups[groupIndex] = updatedGroup;
    return updatedGroup;
};

export const removeUserFromGroup = (groupId: number, userId: number) => {
    const groupIndex = recipientGroups.findIndex(g => g.id === groupId);
    if (groupIndex === -1) return null;

    const group = recipientGroups[groupIndex];
    if (!group.users.find(u => u.id === userId)) return group;

    const updatedGroup = {
        ...group,
        users: group.users.filter(u => u.id !== userId),
        usersCount: Math.max(0, group.usersCount - 1)
    };
    recipientGroups[groupIndex] = updatedGroup;
    return updatedGroup;
};
