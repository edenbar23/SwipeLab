// TypeScript types
export type SwipeDirection = 'yes' | 'no' | 'dont-know' | 'trash';

export interface Question {
  id: string;
  text: string;
  imageUrl: string;
  referenceImages: string[];
}

export interface SwipeResult {
  questionId: string;
  answer: SwipeDirection;
  timestamp: Date;
}

