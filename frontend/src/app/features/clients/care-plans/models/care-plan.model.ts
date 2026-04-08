export type CarePlanStatus = 'ACTIVE' | 'CLOSED' | 'ARCHIVED';
export type GoalStatus = 'PENDING' | 'IN_PROGRESS' | 'ACHIEVED' | 'ABANDONED' | 'PAUSED';
export type AlertSeverity = 'WARNING' | 'ALERT';
export type InterventionStatus = 'ACTIVE' | 'COMPLETED' | 'DISCONTINUED';

export interface InterventionResponse {
  id: string;
  interventionType: string;
  description: string;
  frequency?: string;
  status: InterventionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MilestoneResponse {
  id: string;
  description: string;
  targetDate?: string;
  achievedAt?: string;
  achievedByUserId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface GoalResponse {
  id: string;
  description: string;
  priority: number;
  targetDate?: string;
  status: GoalStatus;
  createdAt: string;
  updatedAt: string;
  interventions: InterventionResponse[];
  milestones: MilestoneResponse[];
}

export interface CarePlanSummary {
  id: string;
  clientId: string;
  therapistId: string;
  title: string;
  description?: string;
  status: CarePlanStatus;
  goalCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CarePlanDetail {
  id: string;
  clientId: string;
  therapistId: string;
  title: string;
  description?: string;
  status: CarePlanStatus;
  closedAt?: string;
  closedByUserId?: string;
  archivedAt?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  goals: GoalResponse[];
}

export interface CarePlanAuditEntry {
  id: string;
  carePlanId: string;
  entityType: string;
  entityId?: string;
  actionType: string;
  actorUserId: string;
  actorName: string;
  actionTimestamp: string;
  fieldName?: string;
  oldValue?: string;
  newValue?: string;
}

// ---- Request payloads ----

export interface CreateInterventionRequest {
  interventionType: string;
  description: string;
  frequency?: string;
}

export interface CreateMilestoneRequest {
  description: string;
  targetDate?: string;
}

export interface CreateGoalRequest {
  description: string;
  priority?: number;
  targetDate?: string;
  interventions?: CreateInterventionRequest[];
  milestones?: CreateMilestoneRequest[];
}

export interface CreateCarePlanRequest {
  title: string;
  description?: string;
  goals: CreateGoalRequest[];
}

export interface UpdateCarePlanRequest {
  title?: string;
  description?: string;
}

export interface UpdateGoalStatusRequest {
  status: GoalStatus;
}

// ---- Progress notes ----

export interface GoalProgressNote {
  id: string;
  goalId: string;
  noteText: string;
  authorName: string;
  createdAt: string;
}

export interface CreateProgressNoteRequest {
  noteText: string;
}

export interface StatusChangeEvent {
  timestamp: string;
  fromStatus: string;
  toStatus: string;
  actorName: string;
}

export interface GoalProgressHistory {
  goalId: string;
  statusHistory: StatusChangeEvent[];
  progressNotes: GoalProgressNote[];
}

// ---- Outcome measures ----

export interface OutcomeMeasureDefinition {
  id: string;
  code: string;
  displayName: string;
  description?: string;
  minScore: number;
  maxScore: number;
  alertThreshold?: number;
  alertLabel?: string;
  alertSeverity?: AlertSeverity;
  sortOrder: number;
}

export interface OutcomeMeasureEntry {
  id: string;
  measureCode: string;
  measureDisplayName: string;
  score: number;
  assessmentDate: string;
  notes?: string;
  thresholdBreached: boolean;
  alertLabel?: string;
  alertSeverity?: AlertSeverity;
  recordedByName: string;
  createdAt: string;
}

export interface RecordOutcomeMeasureRequest {
  measureDefinitionId: string;
  score: number;
  assessmentDate: string;
  notes?: string;
}

export interface OutcomeMeasureChartDataPoint {
  date: string;
  score: number;
  thresholdBreached: boolean;
}

export interface OutcomeMeasureChartData {
  measureCode: string;
  displayName: string;
  alertThreshold?: number;
  series: OutcomeMeasureChartDataPoint[];
}

