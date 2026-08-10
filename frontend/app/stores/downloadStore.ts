import { create } from 'zustand';

export interface DownloadTask {
  taskId: number;
  taskName: string;
}

interface DownloadState {
  activeExports: DownloadTask[];
  addTasks: (tasks: DownloadTask[]) => void;
  removeTasks: (taskIds: number[]) => void;
}

export const useDownloadStore = create<DownloadState>((set) => ({
  activeExports: [],
  addTasks: (tasks) =>
    set((state) => {
      // Prevent duplicates if a user tries to download the same task again while it's in progress
      const existingIds = new Set(state.activeExports.map((t) => t.taskId));
      const newTasks = tasks.filter((t) => !existingIds.has(t.taskId));
      return { activeExports: [...state.activeExports, ...newTasks] };
    }),
  removeTasks: (taskIds) =>
    set((state) => {
      const idsToRemove = new Set(taskIds);
      return {
        activeExports: state.activeExports.filter((t) => !idsToRemove.has(t.taskId)),
      };
    }),
}));
