import { MultiSelectOption } from '../../ui/MultiSelect';

export interface AddTaskFormData {
  name: string;
  description: string;
  speciesList: string[];
  isPublic: boolean;
  selectedRecipients: string[];
  selectedExperiments: string[];
  sharedWithResearchers: string[];
}

export interface StepProps {
  formData: AddTaskFormData;
  onUpdate: (updates: Partial<AddTaskFormData>) => void;
  onNext: () => void;
  onBack?: () => void;
}

export interface StepRecipientsProps extends StepProps {
  availableOptions: MultiSelectOption[];
  availableResearchers: MultiSelectOption[];
  optionsLoading: boolean;
}

export interface StepExperimentsProps extends StepProps {
  availableExperiments: MultiSelectOption[];
  optionsLoading: boolean;
}

export interface StepSpeciesProps extends StepProps {
  availableSpecies?: MultiSelectOption[];
  optionsLoading?: boolean;
}

export interface StepConfirmProps extends StepProps {
  onSubmit: () => void;
  loading: boolean;
  availableOptions: MultiSelectOption[];
}
