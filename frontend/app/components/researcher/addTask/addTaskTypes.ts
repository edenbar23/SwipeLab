import { MultiSelectOption } from '@/components/ui/MultiSelect';

/** One reference image entry — held in local state until task submit. */
export interface SpeciesRefImage {
  /** Set after the image is uploaded to the pool; undefined while still local-only. */
  poolId?: number;
  /** Local file URI (before upload) or backend thumbnail URL (from pool). */
  uri: string;
  /** Optional descriptive caption. */
  caption?: string;
  /** true = image already exists in the backend pool; false = local new upload. */
  fromPool: boolean;
}

export interface AddTaskFormData {
  name: string;
  description: string;
  speciesList: string[];
  /** Maps speciesId → selected/new reference images (1-3 per species). */
  speciesReferenceImages: Record<string, SpeciesRefImage[]>;
  isPublic: boolean;
  selectedRecipients: string[];
  selectedExperiments: string[];
  sharedWithResearchers: string[];
  consensusThreshold: number;
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
  /** Pool images keyed by labelId — fetched by AddTaskScreen after species selection. */
  poolImages?: Record<string, { id: number; thumbnailUrl: string; imageUrl: string; caption?: string }[]>;
  poolImagesLoading?: boolean;
}

export interface StepConfirmProps extends StepProps {
  onSubmit: () => void;
  loading: boolean;
  availableOptions: MultiSelectOption[];
}
