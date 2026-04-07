export type NoteType = 'FREE_FORM' | 'STRUCTURED';
export type NoteVisibility = 'PRIVATE' | 'SUPERVISOR_VISIBLE';

export interface SessionNote {
  id: string;
  sessionRecordId: string;
  noteType: NoteType;
  visibility: NoteVisibility;
  content: string | null;
  structuredFields: Record<string, string> | null;
  authorName: string;
  createdAt: string;
  updatedAt: string;
  hasVersionHistory: boolean;
}

export interface NoteVersion {
  id: string;
  versionNumber: number;
  content: string | null;
  structuredFields: Record<string, string> | null;
  authorName: string;
  createdAt: string;
}

export interface NoteTemplate {
  id: string;
  name: string;
  description: string | null;
  fields: NoteTemplateField[];
}

export interface NoteTemplateField {
  key: string;
  label: string;
  required: boolean;
}

export interface CreateNoteRequest {
  noteType: NoteType;
  visibility: NoteVisibility;
  content?: string;
  structuredFields?: Record<string, string>;
}

export interface UpdateNoteRequest {
  visibility?: NoteVisibility;
  content?: string;
  structuredFields?: Record<string, string>;
}
