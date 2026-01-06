export interface User {
  id: number;
  username: string;
  description?: string; // e.g. "Student" or "Expert"
}

export const usersMock: User[] = [
  { id: 101, username: 'user_1', description: 'Student' },
  { id: 102, username: 'user_2', description: 'Student' },
  { id: 103, username: 'expert_1', description: 'Expert' },
  { id: 104, username: 'volunteer_a', description: 'Volunteer' },
  { id: 105, username: 'volunteer_b', description: 'Volunteer' },
  { id: 106, username: 'researcher_x', description: 'Researcher' },
  { id: 107, username: 'researcher_y', description: 'Researcher' },
  { id: 108, username: 'student_bonus', description: 'Student' },
  // Add more as needed
];
