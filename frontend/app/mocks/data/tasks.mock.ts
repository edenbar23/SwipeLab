export const tasksMock = {
  myTasks: {
    page: 1,
    pageSize: 20,
    totalPages: 1,
    totalTasks: 1,
    tasks: [
      {
        taskId: 7,
        name: 'Asian Giant Hornet Identification',
        description: 'Identify Asian Giant Hornets in trap images',
        species: [
          {
            name: 'Vespa mandarinia',
            referenceImages: [
              {
                imageUrl: 'https://swipelab.com/ref1.jpg',
                caption: 'Dorsal view'
              }
            ]
          }
        ],
        totalImages: 100,
        imagesClassified: 32
      }
    ]
  },

  taskDetails: {
    taskId: 7,
    name: 'Asian Giant Hornet Identification',
    description: 'Identify Asian Giant Hornets in trap images',
    species: [
      {
        name: 'Vespa mandarinia',
        referenceImages: [
          {
            imageUrl: 'https://swipelab.com/ref1.jpg',
            caption: 'Dorsal view'
          }
        ]
      }
    ],
    totalImages: 100,
    imagesClassified: 32,
    assignedToUser: true,
    status: 'ACTIVE',
    createdAt: '2025-12-15T14:30:00Z',
    deadline: '2026-01-10T23:59:59Z'
  },

  adminTasks: {
    tasks: [
      {
        taskId: 7,
        status: 'ACTIVE',
        name: 'Asian Giant Hornet Identification',
        description: 'Identify Asian Giant Hornets in trap images',
        experiments: [101, 102],
        recipientGroups: [3],
        progress: {
          totalImages: 1000,
          imagesClassified: 320
        },
        minClassificationsPerImage: 3,
        consensusThreshold: 80.0
      }
    ]
  }
}
