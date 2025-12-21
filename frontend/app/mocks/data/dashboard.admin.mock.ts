import { taxonomyMock } from './taxonomy.mock'

export const dashboardAdminMock = {
  tasks: {
    tasks: [
      {
        taskId: 7,
        status: 'ACTIVE',
        name: 'Asian Giant Hornet Identification',
        description: 'Identify Asian Giant Hornets in trap images',
        targetSpecies: [
          {
            name: 'Vespa mandarinia',
            commonName: 'Asian Giant Hornet',
            referenceImages: [
              {
                contentType: 'image/jpeg',
                data: 'BASE64_REFERENCE_IMAGE',
                caption: 'Dorsal view'
              }
            ]
          }
        ],
        experiments: [101, 102],
        recipientGroups: [3, 7],
        progress: {
          totalImages: 1000,
          imagesClassified: 320
        },
        minClassificationsPerImage: 3,
        consensusThreshold: 80.0
      }
    ]
  },

  createTask: {
    taskId: 8,
    status: 'ACTIVE',
    name: 'New Identification Task',
    description: 'New task created by admin',
    experiments: [201],
    recipientGroups: [5],
    progress: {
      totalImages: 500,
      imagesClassified: 0
    },
    minClassificationsPerImage: 3,
    consensusThreshold: 75.0,
    createdAt: new Date().toISOString()
  },

  archiveTask: {
    taskId: 7,
    status: 'ARCHIVED',
    archivedAt: new Date().toISOString()
  },

  taxonomy: taxonomyMock
}
