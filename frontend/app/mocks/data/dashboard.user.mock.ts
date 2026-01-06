export const dashboardUserMock = {

  tasks: {
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

  play: {
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
      },
      {
        imageId: 1235,
        imageBuffer: 'BASE64_IMAGE_DATA',
        contentType: 'image/jpeg',
        question: 'Is this a Vespa mandarinia?',
        taskId: 7,
        species: 'Vespa mandarinia'
      }
    ]
  },

  availableTasks: [
    {
      taskId: 101,
      name: 'Red Fire Ant Control',
      description: 'Help identify invasive fire ants in the southern region.',
      motivation: 'Fire ants cause billions in damages annually. Your help identifying them allows for targeted control, protecting native wildlife and agriculture.',
      species: [
        { name: 'Solenopsis invicta', referenceImages: [] }
      ],
      totalImages: 500,
      imagesClassified: 120,
      progress: 24,
      isJoined: true
    },
    {
      taskId: 102,
      name: 'Urban Butterfly Watch',
      description: 'Classify butterfly species found in urban gardens.',
      motivation: 'Butterflies are key indicators of environmental health. Monitoring their populations helps us understand the impact of urbanization on biodiversity.',
      species: [
        { name: 'Papilio glaucus', referenceImages: [] },
        { name: 'Danaus plexippus', referenceImages: [] }
      ],
      totalImages: 200,
      imagesClassified: 0,
      progress: 0,
      isJoined: false
    },
    {
      taskId: 104,
      name: 'Invasive Beetle Tracker',
      description: 'Track the spread of invasive beetles in forests.',
      motivation: 'Invasive beetles destroy thousands of trees each year. Early detection is our best defense.',
      species: [
        { name: 'Anoplophora glabripennis', referenceImages: [] }
      ],
      totalImages: 300,
      imagesClassified: 0,
      progress: 0,
      isJoined: false
    }
  ]
}
