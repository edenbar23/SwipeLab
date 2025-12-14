import { create } from 'zustand';
import { ImageService } from '../services/imageService';
import { Question, SwipeDirection, SwipeResult } from '../types';

interface SwipeStore {
  questions: Question[];
  currentIndex: number;
  results: SwipeResult[];
  isLoading: boolean;
  
  fetchQuestions: () => Promise<void>;
  submitSwipe: (direction: SwipeDirection) => Promise<void>;
  getCurrentQuestion: () => Question | null;
  reset: () => void;
}

export const useSwipeStore = create<SwipeStore>((set, get) => ({
  questions: [],
  currentIndex: 0,
  results: [],
  isLoading: false,

  fetchQuestions: async () => {
    set({ isLoading: true });
    try {
      const questions = await ImageService.fetchImages();
      set({ questions, currentIndex: 0 });
    } catch (error) {
      console.error('Failed to fetch questions:', error);
    } finally {
      set({ isLoading: false });
    }
  },

  submitSwipe: async (direction: SwipeDirection) => {
    const { questions, currentIndex } = get();
    const currentQuestion = questions[currentIndex];

    if (!currentQuestion) return;

    const result: SwipeResult = {
      questionId: currentQuestion.id,
      answer: direction,
      timestamp: new Date(),
    };

    try {
      await ImageService.submitAnswer(result);
      set((state) => ({
        results: [...state.results, result],
        currentIndex: state.currentIndex + 1,
      }));
    } catch (error) {
      console.error('Failed to submit answer:', error);
    }
  },

  getCurrentQuestion: () => {
    const { questions, currentIndex } = get();
    return questions[currentIndex] || null;
  },

  reset: () => {
    set({ questions: [], currentIndex: 0, results: [] });
  },
}));