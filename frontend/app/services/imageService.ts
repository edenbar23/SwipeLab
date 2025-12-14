import { Question, SwipeResult } from '../types';

export class ImageService {
  // Mock service - replace with actual API calls
  
  static async fetchImages(): Promise<Question[]> {
    // TODO: Implement API call
    return [
      {
        id: '1',
        text: 'Is this a dog?',
        imageUrl: 'https://example.com/image1.jpg',
        referenceImages: [
          'https://example.com/ref1.jpg',
          'https://example.com/ref2.jpg',
          'https://example.com/ref3.jpg',
          'https://example.com/ref4.jpg',
        ],
      },
    ];
  }

  static async submitAnswer(result: SwipeResult): Promise<void> {
    // TODO: Implement API call
    console.log('Submitting answer:', result);
  }
}