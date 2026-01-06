import { useThemeStore } from '../stores/themeStore';

export function useColorScheme() {
  const { theme } = useThemeStore();
  return theme as 'light' | 'dark';
}
