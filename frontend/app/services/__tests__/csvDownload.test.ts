import { downloadCsvBlob } from '@/services/csvDownload';
import { Platform } from 'react-native';

// Mock expo modules for mobile path
jest.mock('expo-file-system', () => ({
  documentDirectory: '/mock/docs/',
  writeAsStringAsync: jest.fn(() => Promise.resolve()),
  EncodingType: { UTF8: 'utf8' },
}), { virtual: true });

jest.mock('expo-sharing', () => ({
  shareAsync: jest.fn(() => Promise.resolve()),
}), { virtual: true });

describe('downloadCsvBlob', () => {

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('web platform', () => {
    beforeAll(() => {
      (Platform as any).OS = 'web';
    });

    it('creates a download link and triggers click', async () => {
      const mockClick = jest.fn();
      const mockCreateElement = jest.spyOn(document, 'createElement').mockReturnValue({
        href: '',
        download: '',
        click: mockClick,
      } as any);
      const mockAppendChild = jest.spyOn(document.body, 'appendChild').mockImplementation((n) => n);
      const mockRemoveChild = jest.spyOn(document.body, 'removeChild').mockImplementation((n) => n);
      const mockCreateObjectURL = jest.fn(() => 'blob:mock-url');
      const mockRevokeObjectURL = jest.fn();
      global.URL.createObjectURL = mockCreateObjectURL;
      global.URL.revokeObjectURL = mockRevokeObjectURL;

      const blob = new Blob(['id,name\n1,test'], { type: 'text/csv' });
      await downloadCsvBlob(blob, 'export.csv');

      expect(mockCreateElement).toHaveBeenCalledWith('a');
      expect(mockClick).toHaveBeenCalled();
      expect(mockRevokeObjectURL).toHaveBeenCalledWith('blob:mock-url');

      mockCreateElement.mockRestore();
      mockAppendChild.mockRestore();
      mockRemoveChild.mockRestore();
    });
  });

  describe('mobile platform', () => {
    beforeAll(() => {
      (Platform as any).OS = 'ios';
    });

    afterAll(() => {
      (Platform as any).OS = 'web';
    });

    it('writes to file system and opens share sheet', async () => {
      const FileSystem = require('expo-file-system');
      const Sharing = require('expo-sharing');

      const csvContent = 'id,name\n1,test';
      const blob = new Blob([csvContent], { type: 'text/csv' });

      await downloadCsvBlob(blob, 'export.csv');

      expect(FileSystem.writeAsStringAsync).toHaveBeenCalledWith(
        '/mock/docs/export.csv',
        csvContent,
        { encoding: 'utf8' }
      );
      expect(Sharing.shareAsync).toHaveBeenCalledWith(
        '/mock/docs/export.csv',
        expect.objectContaining({ mimeType: 'text/csv' })
      );
    });
  });
});
