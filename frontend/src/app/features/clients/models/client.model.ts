/** Full detail returned for a single client record. */
export interface ClientDetail {
  id: string;
  fullName: string;
  sourceLeadId: string | null;
  ownerId: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}
