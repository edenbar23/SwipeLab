export type researcherStackParamList = {
  ResearcherDashboard: undefined;
  TasksManagement: undefined;
  TaskDetails: { taskId: number };
  AddTask: { initialSpecies?: string[] } | undefined;
  EditTask: { taskId: number };
  GoldImagesManagement: undefined;
  AddGoldImage: undefined;
  RecipientsList: undefined;
  RecipientGroupDetails: { group: any };
  Analytics: undefined;
  UsersManagement: undefined;
  AddUser: undefined;
  Taxonomy: undefined;
  UserSettings: undefined;
  Profile: undefined;
};
