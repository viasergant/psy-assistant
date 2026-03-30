/** Full detail returned for a single client record. */
export interface ClientDetail {
  id: string;
  fullName: string;
  clientCode: string | null;
  version: number;
  sourceLeadId: string | null;
  ownerId: string | null;
  assignedTherapistId: string | null;
  notes: string | null;
  preferredName: string | null;
  dateOfBirth: string | null;
  sexOrGender: string | null;
  pronouns: string | null;
  email: string | null;
  phone: string | null;
  secondaryPhone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  region: string | null;
  postalCode: string | null;
  country: string | null;
  referralSource: string | null;
  referralContactName: string | null;
  referralNotes: string | null;
  preferredCommunicationMethod: string | null;
  allowPhone: boolean | null;
  allowSms: boolean | null;
  allowEmail: boolean | null;
  allowVoicemail: boolean | null;
  emergencyContactName: string | null;
  emergencyContactRelationship: string | null;
  emergencyContactPhone: string | null;
  emergencyContactEmail: string | null;
  tags: string[];
  photoUrl: string | null;
  canEditProfile: boolean;
  canEditTags: boolean;
  canUploadPhoto: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}

/** Request body for replacing all client tags. */
export interface UpdateClientTagsPayload {
  version: number;
  tags: string[];
}

/** Request body for updating client profile slice-one fields. */
export interface UpdateClientProfilePayload {
  version: number;
  fullName: string;
  preferredName: string | null;
  dateOfBirth: string | null;
  sexOrGender: string | null;
  pronouns: string | null;
  ownerId: string | null;
  assignedTherapistId: string | null;
  notes: string | null;
  email: string | null;
  phone: string | null;
  secondaryPhone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  region: string | null;
  postalCode: string | null;
  country: string | null;
  referralSource: string | null;
  referralContactName: string | null;
  referralNotes: string | null;
  preferredCommunicationMethod: string | null;
  allowPhone: boolean | null;
  allowSms: boolean | null;
  allowEmail: boolean | null;
  allowVoicemail: boolean | null;
  emergencyContactName: string | null;
  emergencyContactRelationship: string | null;
  emergencyContactPhone: string | null;
  emergencyContactEmail: string | null;
}
