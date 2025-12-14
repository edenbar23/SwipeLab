import { useWindowDimensions } from 'react-native';

export default function useResponsive() {
  const { width, height } = useWindowDimensions();

  // common breakpoints (industry standard)
  const isSmallPhone = width < 360;
  const isPhone = width < 600;
  const isTablet = width >= 600 && width < 900;
  const isDesktop = width >= 900;

  return {
    width,
    height,
    isSmallPhone,
    isPhone,
    isTablet,
    isDesktop,
  };
}
