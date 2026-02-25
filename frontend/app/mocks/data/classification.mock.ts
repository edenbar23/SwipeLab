export const classificationMock = {

  nextBatch: {
    images: [
      {
        imageId: 1234,
        taskId: 7,
        question: 'Is this a Vespa mandarinia?',
        image: {
          contentType: 'image/jpeg',
          data: 'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000'
        },
        referenceImages: [
          {
            contentType: 'image/jpeg',
            data: 'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
            caption: 'Example of Vespa mandarinia'
          },
          {
            contentType: 'image/jpeg',
            data: 'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
            caption: 'Example of Vespa mandarinia'
          },
          {
            contentType: 'image/jpeg',
            data: 'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
            caption: 'Example of Vespa mandarinia'
          }
        ]
      },
      {
        imageId: 1235,
        taskId: 7,
        question: 'Is this a Vespa mandarinia?',
        image: {
          contentType: 'image/jpeg',
          data: 'https://images.unsplash.com/photo-1541814631336-e069c9b60b64'
        },
        referenceImages: [
          {
            contentType: 'image/jpeg',
            data: 'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
            caption: 'Reference back'
          }
        ]
      },
      {
        imageId: 1236,
        taskId: 7,
        question: 'Is this a Vespa mandarinia?',
        image: {
          contentType: 'image/jpeg',
          data: 'https://images.unsplash.com/photo-1620188989508-30aa0743b591'
        },
        referenceImages: [
          {
            contentType: 'image/jpeg',
            data: 'https://snworksceo.imgix.net/ids/673119a2-05dd-4329-a9cc-9cc0f4496267.sized-1000x1000.png?w=1000',
            caption: 'Reference back'
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
