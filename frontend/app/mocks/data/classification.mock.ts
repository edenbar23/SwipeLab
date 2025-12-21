export const classificationMock = {

  nextBatch: {
    images: [
      {
        imageId: 1234,
        taskId: 7,
        question: 'Is this a Vespa mandarinia?',
        image: {
          contentType: 'image/jpeg',
          data: 'BASE64_IMAGE_DATA'
        },
        referenceImages: [
          {
            contentType: 'image/jpeg',
            data: 'BASE64_REFERENCE_IMAGE',
            caption: 'Example of Vespa mandarinia'
          }
        ]
      }
    ]
  },

  submitResult: {
    images: [
      {
        id: 1234,
        taskId: 7,
        question: 'Is this a Vespa mandarinia?',
        image: {
          contentType: 'image/jpeg',
          data: 'BASE64_IMAGE_DATA'
        }
      }
    ],
    referenceImages: [
      {
        contentType: 'image/jpeg',
        data: 'BASE64_REFERENCE_IMAGE',
        caption: 'Example of Vespa mandarinia'
      }
    ]
  },

  progress: {
    completed: 120,
    accuracy: 0.91
  },

  playTask: {
    taskId: 7,
    species: [
      {
        scientificName: 'Vespa mandarinia',
        commonName: 'Asian Giant Hornet',
        referenceImages: [
          {
            imageUrl: 'https://stardbi.com/ref1.jpg',
            caption: 'Dorsal view'
          }
        ]
      }
    ],
    images: [
      {
        imageId: 1234,
        imageBuffer: 'BASE64_IMAGE_DATA',
        contentType: 'image/jpeg',
        question: 'Is this a Vespa mandarinia?',
        taskId: 7,
        species: 'Vespa mandarinia'
      }
    ]
  }
}
