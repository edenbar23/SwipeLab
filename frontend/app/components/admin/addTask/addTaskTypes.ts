import { MultiSelectOption } from '../../ui/MultiSelect';

export interface AddTaskFormData {
  name: string;
  description: string;
  speciesList: string[];
  isPublic: boolean;
  selectedRecipients: string[];
}

export interface StepProps {
  formData: AddTaskFormData;
  onUpdate: (updates: Partial<AddTaskFormData>) => void;
  onNext: () => void;
  onBack?: () => void;
}

export interface StepRecipientsProps extends StepProps {
  availableOptions: MultiSelectOption[];
  optionsLoading: boolean;
}

export interface StepConfirmProps extends StepProps {
  onSubmit: () => void;
  loading: boolean;
  availableOptions: MultiSelectOption[];
}
