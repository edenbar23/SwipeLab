import { ImageSourcePropType } from "react-native";

export type ScreenHeaderLayoutProps = {
  leftIcon: ImageSourcePropType;
  leftTitle: string;

  rightIcon: ImageSourcePropType;
  rightTitle: string;
  onRightPress?: () => void;

  children: React.ReactNode;
};
