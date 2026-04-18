import { ImageSourcePropType, StyleProp, ViewStyle } from "react-native";

export type ScreenHeaderLayoutProps = {
  leftIcon: ImageSourcePropType | React.ReactNode;
  leftTitle: string;
  onLeftPress?: () => void;

  rightIcon: ImageSourcePropType | React.ReactNode;
  rightTitle: string;
  onRightPress?: () => void;

  centerIcon?: ImageSourcePropType | React.ReactNode;
  centerTitle?: string;
  onCenterPress?: () => void;

  contentContainerStyle?: StyleProp<ViewStyle>;

  children: React.ReactNode;
};
