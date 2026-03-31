# PA-29 Scheduling Calendar UI Design Specification

## Design Philosophy

**Aesthetic Direction**: Refined editorial with warm professionalism  
**Core Principle**: Create a calendar interface that feels like a thoughtfully designed journal, not a generic grid

### Visual Identity
- **Typography**: Cormorant Garamond (display) + Work Sans (UI text) — elegant serif paired with functional sans
- **Color Palette**: Warm earth tones with accent contrast
  - Primary: `#2C5F4F` (deep forest green)
  - Secondary: `#E8DCC4` (warm sand)
  - Accent: `#D97642` (terracotta)
  - Surface: `#FAFAF8` (off-white)
  - Text: `#1A1A1A` (near-black)
  - Subtle: `#C8BCB0` (warm gray)
- **Spatial Philosophy**: Generous breathing room with intentional density in data zones
- **Motion Language**: Smooth, organic transitions (cubic-bezier) emphasizing state changes

---

## Component Architecture

### 1. Schedule Overview (Admin View)

**Layout Structure**: Split-panel composition with therapist roster on left, calendar canvas on right.

```
┌─────────────────────────────────────────────────────────────┐
│  Therapist Schedules          [+ New Schedule]  [This Month] │
├──────────────┬──────────────────────────────────────────────┤
│              │                                               │
│  Therapists  │           Calendar Canvas                     │
│  Sidebar     │           (Week/Month View)                   │
│  (Filterable)│                                               │
│              │                                               │
│              │                                               │
│              │                                               │
└──────────────┴──────────────────────────────────────────────┘
```

#### Left Sidebar: Therapist Roster
- Vertically scrollable list
- Each therapist card shows:
  - Name + profile photo (circular, 48px)
  - Specialization badge (subtle, pill-shaped)
  - Timezone indicator (small, icon + text)
  - Status indicator (available/on leave) using color-coded dot
- Active selection: Left border (`4px solid #2C5F4F`) + background tint
- Hover state: Smooth lift effect with subtle shadow
- Search filter at top with real-time results

#### Right Canvas: Calendar View

**Week View** (Primary)
- 7-day horizontal layout, Monday start
- Time slots in 30-minute increments (06:00 - 22:00 configurable)
- Visual language:
  - **Available slots**: Light green tint (`#E8F5E9`) with subtle border
  - **Unavailable**: Diagonal stripe pattern (CSS) in warm gray
  - **Leave periods**: Solid terracotta block with rounded corners
  - **Overrides**: Amber border + icon indicator
  - **Booked appointments**: Deep green with white text, showing duration bar

**Month View** (Secondary)
- Traditional calendar grid
- Days with availability: Small green dot indicator
- Leave days: Terracotta background tint
- Override days: Amber dot indicator
- Clicking day opens day-detail modal

---

### 2. Schedule Configuration Panel (Admin)

**Modal/Slide-in Panel** (right-side drawer, 480px width)

#### Section A: Recurring Weekly Schedule
```
┌──────────────────────────────────────────────┐
│  Weekly Working Hours                        │
│                                              │
│  Monday     ▢  09:00  —  17:00  [×]         │
│  Tuesday    ▢  09:00  —  17:00  [×]         │
│  Wednesday  ☑  09:00  —  17:00  [×]         │
│  Thursday   ☑  09:00  —  17:00  [×]         │
│  Friday     ☑  09:00  —  15:00  [×]         │
│  Saturday   ▢  Unavailable                   │
│  Sunday     ▢  Unavailable                   │
│                                              │
│  [+ Add Exception]                           │
└──────────────────────────────────────────────┘
```

- Days are checkboxes with inline time pickers
- Time pickers: Dropdown with 30-min increments, smooth scroll
- Visual: Clean rows with alternating subtle background tint
- Unchecked days automatically grayed out
- Remove button (×) appears on hover only

#### Section B: Schedule Overrides
```
┌──────────────────────────────────────────────┐
│  Specific Date Overrides                     │
│                                              │
│  📅 Apr 15, 2026                            │
│  ⊗  Unavailable (Conference)                │
│  [Edit] [Delete]                            │
│                                              │
│  📅 Apr 22, 2026                            │
│  ⏰ 14:00 — 18:00 (Extended hours)          │
│  [Edit] [Delete]                            │
│                                              │
│  [+ Add Override]                           │
└──────────────────────────────────────────────┘
```

- Card-based list, newest first
- Each override: Date, status icon, description, action buttons
- Subtle shadow on hover
- Add button opens date-picker + time-picker modal

#### Section C: Leave Management
```
┌──────────────────────────────────────────────┐
│  Leave Periods                               │
│                                              │
│  🏖️ Annual Leave                            │
│  May 1 — May 14, 2026 (Approved)           │
│  [View Details] [Cancel]                    │
│                                              │
│  🤒 Sick Leave                              │
│  Mar 28 — Mar 29, 2026 (Pending)           │
│  [Approve] [Reject]                         │
│                                              │
│  [+ Request Leave]                          │
└──────────────────────────────────────────────┘
```

- Visual status indicators:
  - **Approved**: Green left-border stripe
  - **Pending**: Amber left-border stripe, pulsing glow
  - **Rejected**: Red left-border stripe
- Leave type icons (emoji or custom SVG)
- Grouped by status (Pending → Approved → Past)

---

### 3. Therapist Self-Service View

**Calendar View with Role-Based Editing Permissions**

```
┌─────────────────────────────────────────────────────────────┐
│  My Schedule      [Configure Schedule]  [Request Leave]  [▼]│
├─────────────────────────────────────────────────────────────┤
│                                                              │
│                Calendar (Week View)                          │
│                Color-coded with role-based interaction       │
│                                                              │
│  Legend:                                                     │
│  ● Available   ● Booked   ● Override   ● Leave             │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│  Upcoming Leave & Overrides                                  │
│                                                              │
│  📅 Apr 15, 2026 — Conference (Unavailable)                │
│  🏖️ May 1-14, 2026 — Annual Leave (Approved)              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Permission Logic**:
- Therapists see only their own schedule data (enforced by role filter)
- **If therapist has Receptionist role**: Full editing capabilities including:
  - Modify recurring weekly schedule
  - Create and edit schedule overrides
  - Manage leave periods without approval workflow
  - "Configure Schedule" button is visible and active
- **If therapist has only Therapist role**: Limited self-service capabilities:
  - Read-only calendar view
  - Can submit leave requests (requires admin approval)
  - "Configure Schedule" button is hidden
- "Request Leave" opens streamlined modal  
- Clear legend with color coding

---

### 4. Leave Request Modal (Therapist)

**Centered modal, 440px width, soft rounded corners**

```
┌──────────────────────────────────────────────┐
│  Request Leave                         [×]   │
│                                              │
│  Leave Type                                  │
│  [▼ Select type...                        ]  │
│                                              │
│  Date Range                                  │
│  From: [📅 Apr 1, 2026                    ]  │
│  To:   [📅 Apr 5, 2026                    ]  │
│                                              │
│  Reason (optional)                           │
│  ┌────────────────────────────────────────┐  │
│  │                                        │  │
│  │                                        │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ⚠ 2 existing appointments in this range    │
│  [View Conflicts]                            │
│                                              │
│            [Cancel]  [Submit Request]        │
└──────────────────────────────────────────────┘
```

- Conflict detection in real-time as dates are selected
- Warning box appears with count if conflicts exist
- "View Conflicts" expands to show appointment details
- Smooth fade-in animation on open

---

### 5. Conflict Warning Component

**Inline alert box with detailed breakdown**

```
┌──────────────────────────────────────────────┐
│  ⚠ Scheduling Conflicts Detected             │
│                                              │
│  The following appointments overlap with     │
│  your requested leave period:                │
│                                              │
│  • Wed, Apr 3, 10:00 — Sarah Mitchell        │
│    (Initial Consultation)                    │
│                                              │
│  • Fri, Apr 5, 14:00 — James Chen            │
│    (Follow-up Session)                       │
│                                              │
│  Please coordinate with clients before       │
│  finalizing this leave request.              │
└──────────────────────────────────────────────┘
```

- Warm amber background (`#FFF8E1`)
- Terracotta border-left accent
- Clear, scannable list format
- Actionable guidance

---

### 6. Availability Slot Visual Language

**Calendar cell states** (applies to both week and month views):

| State | Visual Treatment |
|-------|-----------------|
| **Available** | Light tint (`#E8F5E9`), subtle border, hover brightens |
| **Partially Available** | Gradient fill (available→unavailable), split indicator |
| **Unavailable (Recurring)** | Diagonal stripe pattern, muted gray |
| **Leave** | Solid terracotta block, rounded `border-radius: 8px`, label |
| **Override** | Amber `border: 2px solid`, small badge icon (top-right) |
| **Booked** | Deep green, white text, duration bar with time label |

**Interaction States**:
- Hover: Lift + shadow (`box-shadow: 0 4px 12px rgba(0,0,0,0.08)`)
- Active (editing): Thick border pulse animation
- Selected: Deep green border + background tint

---

## Component Specifications

### Calendar Week View Component

**Angular Standalone Component**: `schedule-calendar.component.ts`

```typescript
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

interface TimeSlot {
  startTime: string;
  endTime: string;
  status: 'available' | 'unavailable' | 'leave' | 'override' | 'booked';
  metadata?: {
    leaveType?: string;
    appointmentDetails?: string;
    overrideReason?: string;
  };
}

interface DaySchedule {
  date: Date;
  slots: TimeSlot[];
}

@Component({
  selector: 'app-schedule-calendar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './schedule-calendar.component.html',
  styleUrls: ['./schedule-calendar.component.scss']
})
export class ScheduleCalendarComponent {
  @Input() weekData: DaySchedule[] = [];
  @Input() readOnly: boolean = false;
  @Input() userRoles: string[] = [];
  @Input() therapistTimezone: string = 'UTC';
  @Output() slotClick = new EventEmitter<{date: Date, slot: TimeSlot}>();
  
  // Computed property: therapist can edit if they have receptionist role
  get canEditSchedule(): boolean {
    return !this.readOnly && this.userRoles.includes('RECEPTIONIST');
  }
  
  // Component logic here
}
```

### HTML Template (Excerpt)

```html
<div class="schedule-calendar">
  <header class="calendar-header">
    <h1 class="calendar-title">Therapist Schedule</h1>
    <div class="calendar-controls">
      <button class="btn-icon" (click)="previousWeek()">
        <i class="pi pi-chevron-left"></i>
      </button>
      <span class="week-label">{{ currentWeekLabel }}</span>
      <button class="btn-icon" (click)="nextWeek()">
        <i class="pi pi-chevron-right"></i>
      </button>
    </div>
    <button 
      class="btn-primary" 
      (click)="openScheduleConfig()"
      *ngIf="canEditSchedule">
      Configure Schedule
    </button>
  </header>

  <div class="calendar-grid">
    <div class="time-axis">
      <div class="time-label" *ngFor="let hour of timeSlots">
        {{ hour }}
      </div>
    </div>

    <div class="days-container">
      <div class="day-column" *ngFor="let day of weekData">
        <div class="day-header">
          <span class="day-name">{{ day.date | date:'EEE' }}</span>
          <span class="day-date">{{ day.date | date:'MMM d' }}</span>
        </div>
        
        <div class="slots-container">
          <div 
            class="slot"
            *ngFor="let slot of day.slots"
            [ngClass]="'slot--' + slot.status"
            [class.slot--read-only]="!canEditSchedule"
            [attr.data-tooltip]="getSlotTooltip(slot)"
            (click)="canEditSchedule ? onSlotClick(day.date, slot) : null"
            [@slotFade]>
            
            <span class="slot-time" *ngIf="slot.status === 'booked'">
              {{ slot.startTime }}
            </span>
            
            <span class="slot-label" *ngIf="slot.metadata?.leaveType">
              {{ slot.metadata.leaveType }}
            </span>
            
            <i class="slot-icon pi pi-exclamation-circle" 
               *ngIf="slot.status === 'override'"></i>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="calendar-legend">
    <div class="legend-item">
      <span class="legend-dot legend-dot--available"></span>
      <span>Available</span>
    </div>
    <div class="legend-item">
      <span class="legend-dot legend-dot--booked"></span>
      <span>Booked</span>
    </div>
    <div class="legend-item">
      <span class="legend-dot legend-dot--leave"></span>
      <span>On Leave</span>
    </div>
    <div class="legend-item">
      <span class="legend-dot legend-dot--override"></span>
      <span>Override</span>
    </div>
    <div class="legend-item">
      <span class="legend-dot legend-dot--unavailable"></span>
      <span>Unavailable</span>
    </div>
  </div>
</div>
```

### SCSS Styles (schedule-calendar.component.scss)

```scss
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;600;700&family=Work+Sans:wght@300;400;500;600&display=swap');

:root {
  --color-primary: #2C5F4F;
  --color-secondary: #E8DCC4;
  --color-accent: #D97642;
  --color-surface: #FAFAF8;
  --color-text: #1A1A1A;
  --color-subtle: #C8BCB0;
  
  --color-available: #E8F5E9;
  --color-available-border: #A5D6A7;
  --color-booked: #2C5F4F;
  --color-leave: #D97642;
  --color-override: #F9A825;
  --color-unavailable: #E0E0E0;
  
  --font-display: 'Cormorant Garamond', serif;
  --font-ui: 'Work Sans', sans-serif;
  
  --transition-smooth: cubic-bezier(0.4, 0, 0.2, 1);
  --shadow-soft: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-lifted: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.schedule-calendar {
  background: var(--color-surface);
  min-height: 100vh;
  font-family: var(--font-ui);
  color: var(--color-text);
  padding: 2rem;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 2px solid var(--color-secondary);
}

.calendar-title {
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: 600;
  color: var(--color-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.calendar-controls {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  
  .btn-icon {
    background: white;
    border: 1px solid var(--color-subtle);
    border-radius: 50%;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s var(--transition-smooth);
    
    &:hover {
      background: var(--color-primary);
      border-color: var(--color-primary);
      color: white;
      transform: scale(1.08);
      box-shadow: var(--shadow-lifted);
    }
  }
  
  .week-label {
    font-weight: 500;
    font-size: 1.1rem;
    min-width: 180px;
    text-align: center;
    color: var(--color-text);
  }
}

.btn-primary {
  background: var(--color-primary);
  color: white;
  border: none;
  padding: 0.75rem 1.75rem;
  border-radius: 8px;
  font-family: var(--font-ui);
  font-weight: 500;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.3s var(--transition-smooth);
  box-shadow: var(--shadow-soft);
  
  &:hover {
    background: darken(#2C5F4F, 8%);
    transform: translateY(-2px);
    box-shadow: var(--shadow-lifted);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.calendar-grid {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
}

.time-axis {
  display: flex;
  flex-direction: column;
  padding-top: 60px; // Offset for day headers
  
  .time-label {
    height: 60px; // 30min slots * 2
    display: flex;
    align-items: flex-start;
    font-size: 0.85rem;
    color: var(--color-subtle);
    font-weight: 400;
    padding-top: 0.25rem;
  }
}

.days-container {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.75rem;
}

.day-column {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  transition: box-shadow 0.3s var(--transition-smooth);
  
  &:hover {
    box-shadow: var(--shadow-lifted);
  }
}

.day-header {
  background: linear-gradient(135deg, var(--color-primary) 0%, darken(#2C5F4F, 10%) 100%);
  color: white;
  padding: 1rem;
  text-align: center;
  
  .day-name {
    display: block;
    font-family: var(--font-display);
    font-size: 1.2rem;
    font-weight: 600;
    letter-spacing: 0.03em;
  }
  
  .day-date {
    display: block;
    font-size: 0.9rem;
    opacity: 0.9;
    margin-top: 0.25rem;
  }
}

.slots-container {
  padding: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.slot {
  height: 28px; // 30min slot representation
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  transition: all 0.25s var(--transition-smooth);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  overflow: hidden;
  
  &:hover {
    transform: scale(1.02);
    z-index: 10;
    box-shadow: var(--shadow-soft);
  }
  
  // State-specific styling
  &--available {
    background: var(--color-available);
    border: 1px solid var(--color-available-border);
    
    &:hover {
      background: lighten(#E8F5E9, 2%);
    }
  }
  
  &--unavailable {
    background: repeating-linear-gradient(
      45deg,
      var(--color-unavailable),
      var(--color-unavailable) 4px,
      transparent 4px,
      transparent 8px
    );
    cursor: not-allowed;
    opacity: 0.6;
    
    &:hover {
      transform: none;
    }
  }
  
  &--leave {
    background: var(--color-leave);
    color: white;
    font-weight: 500;
    border: none;
    
    .slot-label {
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
  }
  
  &--override {
    background: #FFF8E1;
    border: 2px solid var(--color-override);
    position: relative;
    
    .slot-icon {
      position: absolute;
      top: 2px;
      right: 2px;
      font-size: 0.7rem;
      color: var(--color-override);
    }
  }
  
  &--booked {
    background: var(--color-booked);
    color: white;
    font-weight: 500;
    border: none;
    
    .slot-time {
      font-size: 0.7rem;
    }
  }
  
  // Read-only state (when user doesn't have edit permissions)
  &--read-only {
    cursor: default;
    
    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}

// Animations
@keyframes slotFade {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.slot {
  animation: slotFade 0.4s var(--transition-smooth);
  animation-fill-mode: both;
  
  @for $i from 1 through 20 {
    &:nth-child(#{$i}) {
      animation-delay: #{$i * 0.02}s;
    }
  }
}

.calendar-legend {
  display: flex;
  gap: 2rem;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow-soft);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: var(--color-text);
}

.legend-dot {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  
  &--available {
    background: var(--color-available);
    border: 1px solid var(--color-available-border);
  }
  
  &--booked {
    background: var(--color-booked);
  }
  
  &--leave {
    background: var(--color-leave);
  }
  
  &--override {
    background: #FFF8E1;
    border: 2px solid var(--color-override);
  }
  
  &--unavailable {
    background: repeating-linear-gradient(
      45deg,
      var(--color-unavailable),
      var(--color-unavailable) 3px,
      transparent 3px,
      transparent 6px
    );
  }
}

// Responsive: Mobile view
@media (max-width: 968px) {
  .days-container {
    grid-template-columns: 1fr; // Stack days vertically
    
    .day-column {
      margin-bottom: 1rem;
    }
  }
  
  .calendar-grid {
    grid-template-columns: 60px 1fr;
  }
  
  .calendar-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
}

// Accessibility
.slot {
  &:focus-visible {
    outline: 3px solid var(--color-primary);
    outline-offset: 2px;
  }
}

// Print styles
@media print {
  .calendar-header {
    border-bottom: 1px solid #ccc;
    
    .calendar-controls,
    .btn-primary {
      display: none;
    }
  }
  
  .slot {
    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}
```

---

### Schedule Configuration Panel (Slide-in Drawer)

**Angular Component**: `schedule-config-panel.component.ts`

```html
<div class="config-panel" [@slideIn]>
  <header class="config-header">
    <h2>Schedule Configuration</h2>
    <button class="btn-close" (click)="close()">
      <i class="pi pi-times"></i>
    </button>
  </header>
  
  <div class="config-body">
    <!-- Section A: Recurring Schedule -->
    <section class="config-section">
      <h3 class="section-title">
        <i class="pi pi-calendar"></i>
        Weekly Working Hours
      </h3>
      
      <div class="recurring-schedule">
        <div class="day-row" *ngFor="let day of weekDays">
          <label class="day-checkbox">
            <input 
              type="checkbox" 
              [(ngModel)]="day.enabled"
              (change)="onDayToggle(day)" />
            <span class="checkbox-custom"></span>
            <span class="day-label">{{ day.name }}</span>
          </label>
          
          <div class="time-pickers" *ngIf="day.enabled">
            <select 
              class="time-select"
              [(ngModel)]="day.startTime"
              [disabled]="!day.enabled">
              <option *ngFor="let time of timeOptions" [value]="time">
                {{ time }}
              </option>
            </select>
            
            <span class="time-separator">—</span>
            
            <select 
              class="time-select"
              [(ngModel)]="day.endTime"
              [disabled]="!day.enabled">
              <option *ngFor="let time of timeOptions" [value]="time">
                {{ time }}
              </option>
            </select>
          </div>
          
          <span class="unavailable-label" *ngIf="!day.enabled">
            Unavailable
          </span>
        </div>
      </div>
      
      <button class="btn-secondary" (click)="applyRecurringSchedule()">
        Save Weekly Schedule
      </button>
    </section>
    
    <!-- Section B: Overrides -->
    <section class="config-section">
      <h3 class="section-title">
        <i class="pi pi-exclamation-triangle"></i>
        Schedule Overrides
      </h3>
      
      <div class="overrides-list">
        <div class="override-card" *ngFor="let override of overrides">
          <div class="override-header">
            <span class="override-date">
              📅 {{ override.date | date:'MMM d, yyyy' }}
            </span>
            <span class="override-badge" [ngClass]="'badge--' + override.type">
              {{ override.type }}
            </span>
          </div>
          
          <div class="override-body">
            <p class="override-description">
              {{ override.description }}
            </p>
            <div class="override-time" *ngIf="override.customHours">
              ⏰ {{ override.startTime }} — {{ override.endTime }}
            </div>
          </div>
          
          <div class="override-actions">
            <button class="btn-text" (click)="editOverride(override)">Edit</button>
            <button class="btn-text btn-text--danger" (click)="deleteOverride(override)">
              Delete
            </button>
          </div>
        </div>
        
        <button class="btn-add" (click)="addOverride()">
          <i class="pi pi-plus"></i>
          Add Override
        </button>
      </div>
    </section>
    
    <!-- Section C: Leave Management -->
    <section class="config-section">
      <h3 class="section-title">
        <i class="pi pi-calendar-times"></i>
        Leave Periods
      </h3>
      
      <div class="leave-list">
        <!-- Pending leaves -->
        <div class="leave-group" *ngIf="pendingLeaves.length > 0">
          <h4 class="group-label">Pending Approval</h4>
          
          <div 
            class="leave-card leave-card--pending" 
            *ngFor="let leave of pendingLeaves"
            [@cardPulse]>
            <div class="leave-indicator"></div>
            
            <div class="leave-content">
              <div class="leave-header">
                <span class="leave-icon">{{ getLeaveIcon(leave.type) }}</span>
                <span class="leave-type">{{ leave.type }}</span>
                <span class="leave-status">Pending</span>
              </div>
              
              <div class="leave-dates">
                {{ leave.startDate | date:'MMM d' }} — 
                {{ leave.endDate | date:'MMM d, yyyy' }}
              </div>
              
              <div class="leave-reason" *ngIf="leave.reason">
                {{ leave.reason }}
              </div>
              
              <div class="leave-conflict" *ngIf="leave.conflictCount > 0">
                ⚠️ {{ leave.conflictCount }} appointment(s) scheduled
              </div>
            </div>
            
            <div class="leave-actions">
              <button class="btn-approve" (click)="approveLeave(leave)">
                <i class="pi pi-check"></i>
                Approve
              </button>
              <button class="btn-reject" (click)="rejectLeave(leave)">
                <i class="pi pi-times"></i>
                Reject
              </button>
            </div>
          </div>
        </div>
        
        <!-- Approved leaves -->
        <div class="leave-group">
          <h4 class="group-label">Approved & Active</h4>
          
          <div 
            class="leave-card leave-card--approved" 
            *ngFor="let leave of approvedLeaves">
            <div class="leave-indicator"></div>
            
            <div class="leave-content">
              <div class="leave-header">
                <span class="leave-icon">{{ getLeaveIcon(leave.type) }}</span>
                <span class="leave-type">{{ leave.type }}</span>
              </div>
              
              <div class="leave-dates">
                {{ leave.startDate | date:'MMM d' }} — 
                {{ leave.endDate | date:'MMM d, yyyy' }}
              </div>
            </div>
            
            <button class="btn-text" (click)="viewLeaveDetails(leave)">
              View Details
            </button>
          </div>
        </div>
        
        <button class="btn-add" (click)="requestLeave()">
          <i class="pi pi-plus"></i>
          Request Leave
        </button>
      </div>
    </section>
  </div>
</div>
```

### SCSS for Config Panel

```scss
.config-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 480px;
  background: white;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2rem;
  border-bottom: 2px solid var(--color-secondary);
  
  h2 {
    font-family: var(--font-display);
    font-size: 2rem;
    font-weight: 600;
    color: var(--color-primary);
    margin: 0;
  }
  
  .btn-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: var(--color-subtle);
    cursor: pointer;
    transition: color 0.3s;
    
    &:hover {
      color: var(--color-text);
    }
  }
}

.config-body {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  
  // Custom scrollbar
  &::-webkit-scrollbar {
    width: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: var(--color-surface);
  }
  
  &::-webkit-scrollbar-thumb {
    background: var(--color-subtle);
    border-radius: 4px;
    
    &:hover {
      background: var(--color-primary);
    }
  }
}

.config-section {
  margin-bottom: 3rem;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-primary);
  margin: 0 0 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  
  i {
    font-size: 1.2rem;
  }
}

// Recurring Schedule
.recurring-schedule {
  .day-row {
    display: flex;
    align-items: center;
    padding: 1rem;
    border-radius: 8px;
    margin-bottom: 0.5rem;
    background: var(--color-surface);
    transition: background 0.3s;
    
    &:hover {
      background: var(--color-secondary);
    }
  }
  
  .day-checkbox {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    cursor: pointer;
    min-width: 140px;
    
    input[type="checkbox"] {
      display: none;
    }
    
    .checkbox-custom {
      width: 24px;
      height: 24px;
      border: 2px solid var(--color-subtle);
      border-radius: 6px;
      position: relative;
      transition: all 0.3s var(--transition-smooth);
      
      &::after {
        content: '✓';
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%) scale(0);
        color: white;
        font-weight: 600;
        transition: transform 0.3s var(--transition-smooth);
      }
    }
    
    input:checked + .checkbox-custom {
      background: var(--color-primary);
      border-color: var(--color-primary);
      
      &::after {
        transform: translate(-50%, -50%) scale(1);
      }
    }
    
    .day-label {
      font-weight: 500;
      color: var(--color-text);
    }
  }
  
  .time-pickers {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex: 1;
  }
  
  .time-select {
    padding: 0.5rem 0.75rem;
    border: 1px solid var(--color-subtle);
    border-radius: 6px;
    font-family: var(--font-ui);
    font-size: 0.9rem;
    background: white;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover:not(:disabled) {
      border-color: var(--color-primary);
    }
    
    &:focus {
      outline: none;
      border-color: var(--color-primary);
      box-shadow: 0 0 0 3px rgba(44, 95, 79, 0.1);
    }
    
    &:disabled {
      background: var(--color-surface);
      cursor: not-allowed;
      opacity: 0.5;
    }
  }
  
  .time-separator {
    color: var(--color-subtle);
    font-weight: 300;
  }
  
  .unavailable-label {
    color: var(--color-subtle);
    font-style: italic;
    flex: 1;
  }
}

.btn-secondary {
  width: 100%;
  margin-top: 1.5rem;
  padding: 0.875rem;
  background: var(--color-secondary);
  color: var(--color-primary);
  border: none;
  border-radius: 8px;
  font-family: var(--font-ui);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s var(--transition-smooth);
  
  &:hover {
    background: darken(#E8DCC4, 8%);
    transform: translateY(-1px);
    box-shadow: var(--shadow-soft);
  }
}

// Overrides & Leave Cards
.override-card,
.leave-card {
  background: white;
  border: 1px solid var(--color-subtle);
  border-radius: 10px;
  padding: 1.25rem;
  margin-bottom: 1rem;
  transition: all 0.3s var(--transition-smooth);
  
  &:hover {
    box-shadow: var(--shadow-lifted);
    transform: translateY(-2px);
  }
}

.override-card {
  .override-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
    
    .override-date {
      font-weight: 600;
      color: var(--color-text);
    }
    
    .override-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      
      &.badge--unavailable {
        background: #FFEBEE;
        color: #C62828;
      }
      
      &.badge--custom-hours {
        background: #FFF8E1;
        color: #F57F17;
      }
    }
  }
  
  .override-body {
    margin-bottom: 0.75rem;
    
    .override-description {
      font-size: 0.9rem;
      color: var(--color-text);
      margin: 0 0 0.5rem;
    }
    
    .override-time {
      font-size: 0.85rem;
      color: var(--color-subtle);
    }
  }
  
  .override-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
  }
}

.leave-card {
  position: relative;
  padding-left: 1.75rem;
  
  .leave-indicator {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 6px;
    border-radius: 10px 0 0 10px;
  }
  
  &--pending .leave-indicator {
    background: var(--color-override);
    animation: pulse 2s ease-in-out infinite;
  }
  
  &--approved .leave-indicator {
    background: #4CAF50;
  }
  
  &--rejected .leave-indicator {
    background: #E53935;
  }
  
  .leave-content {
    flex: 1;
  }
  
  .leave-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.5rem;
    
    .leave-icon {
      font-size: 1.4rem;
    }
    
    .leave-type {
      font-weight: 600;
      color: var(--color-text);
      flex: 1;
    }
    
    .leave-status {
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      background: #FFF8E1;
      color: #F57F17;
    }
  }
  
  .leave-dates {
    font-size: 0.95rem;
    color: var(--color-text);
    margin-bottom: 0.5rem;
  }
  
  .leave-reason {
    font-size: 0.85rem;
    color: var(--color-subtle);
    font-style: italic;
    margin-bottom: 0.75rem;
  }
  
  .leave-conflict {
    background: #FFF3E0;
    border-left: 3px solid var(--color-accent);
    padding: 0.5rem 0.75rem;
    border-radius: 4px;
    font-size: 0.85rem;
    color: #E65100;
    margin-top: 0.75rem;
  }
  
  .leave-actions {
    display: flex;
    gap: 0.75rem;
    margin-top: 1rem;
    
    .btn-approve,
    .btn-reject {
      flex: 1;
      padding: 0.625rem;
      border: none;
      border-radius: 6px;
      font-family: var(--font-ui);
      font-weight: 500;
      font-size: 0.9rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      transition: all 0.3s var(--transition-smooth);
    }
    
    .btn-approve {
      background: #4CAF50;
      color: white;
      
      &:hover {
        background: darken(#4CAF50, 8%);
        box-shadow: var(--shadow-soft);
      }
    }
    
    .btn-reject {
      background: #FFEBEE;
      color: #C62828;
      
      &:hover {
        background: darken(#FFEBEE, 5%);
      }
    }
  }
}

.btn-text {
  background: none;
  border: none;
  color: var(--color-primary);
  font-family: var(--font-ui);
  font-weight: 500;
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0.5rem 0.75rem;
  transition: color 0.3s;
  
  &:hover {
    color: darken(#2C5F4F, 12%);
    text-decoration: underline;
  }
  
  &--danger {
    color: #C62828;
    
    &:hover {
      color: darken(#C62828, 12%);
    }
  }
}

.btn-add {
  width: 100%;
  padding: 1rem;
  background: var(--color-surface);
  border: 2px dashed var(--color-subtle);
  border-radius: 10px;
  color: var(--color-primary);
  font-family: var(--font-ui);
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.3s var(--transition-smooth);
  
  &:hover {
    background: var(--color-secondary);
    border-color: var(--color-primary);
    transform: scale(1.01);
  }
}

.group-label {
  font-family: var(--font-ui);
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-subtle);
  margin: 0 0 1rem;
}

// Animations
@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

@keyframes cardPulse {
  0%, 100% {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  50% {
    box-shadow: 0 4px 16px rgba(249, 168, 37, 0.2);
  }
}
```

---

### Leave Request Modal (Therapist View)

```html
<div class="modal-overlay" (click)="closeModal()">
  <div class="modal-container" (click)="$event.stopPropagation()" [@modalFade]>
    <header class="modal-header">
      <h2>Request Leave</h2>
      <button class="btn-close" (click)="closeModal()">
        <i class="pi pi-times"></i>
      </button>
    </header>
    
    <div class="modal-body">
      <div class="form-group">
        <label class="form-label">Leave Type</label>
        <select class="form-select" [(ngModel)]="leaveRequest.type">
          <option value="">Select type...</option>
          <option value="annual">Annual Leave</option>
          <option value="sick">Sick Leave</option>
          <option value="personal">Personal Leave</option>
          <option value="training">Training / Conference</option>
        </select>
      </div>
      
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">From</label>
          <input 
            type="date" 
            class="form-input"
            [(ngModel)]="leaveRequest.startDate"
            (change)="checkConflicts()" />
        </div>
        
        <div class="form-group">
          <label class="form-label">To</label>
          <input 
            type="date" 
            class="form-input"
            [(ngModel)]="leaveRequest.endDate"
            (change)="checkConflicts()" />
        </div>
      </div>
      
      <div class="form-group">
        <label class="form-label">Reason (optional)</label>
        <textarea 
          class="form-textarea"
          [(ngModel)]="leaveRequest.reason"
          placeholder="Brief description..."
          rows="3"></textarea>
      </div>
      
      <!-- Conflict Warning -->
      <div class="conflict-alert" *ngIf="conflicts.length > 0" [@expandCollapse]>
        <div class="alert-header">
          <i class="pi pi-exclamation-triangle"></i>
          <span>{{ conflicts.length }} Scheduling Conflict(s) Detected</span>
        </div>
        
        <div class="conflict-list" *ngIf="showConflictDetails">
          <div class="conflict-item" *ngFor="let conflict of conflicts">
            <div class="conflict-date">
              {{ conflict.datetime | date:'EEE, MMM d, h:mm a' }}
            </div>
            <div class="conflict-client">
              {{ conflict.clientName }} — {{ conflict.sessionType }}
            </div>
          </div>
        </div>
        
        <button 
          class="btn-link" 
          (click)="showConflictDetails = !showConflictDetails">
          {{ showConflictDetails ? 'Hide' : 'View' }} Conflicts
        </button>
        
        <p class="alert-guidance">
          Please coordinate with affected clients before submitting this request.
        </p>
      </div>
    </div>
    
    <footer class="modal-footer">
      <button class="btn-secondary" (click)="closeModal()">
        Cancel
      </button>
      <button 
        class="btn-primary" 
        (click)="submitRequest()"
        [disabled]="!isFormValid()">
        Submit Request
      </button>
    </footer>
  </div>
</div>
```

### Modal Styles

```scss
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(26, 26, 26, 0.6);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2rem;
  border-bottom: 2px solid var(--color-secondary);
  
  h2 {
    font-family: var(--font-display);
    font-size: 1.8rem;
    font-weight: 600;
    color: var(--color-primary);
    margin: 0;
  }
}

.modal-body {
  padding: 2rem;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  gap: 1rem;
  padding: 1.5rem 2rem;
  border-top: 1px solid var(--color-secondary);
  background: var(--color-surface);
  
  button {
    flex: 1;
  }
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-label {
  display: block;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 0.5rem;
  font-size: 0.95rem;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid var(--color-subtle);
  border-radius: 8px;
  font-family: var(--font-ui);
  font-size: 1rem;
  background: white;
  transition: all 0.3s var(--transition-smooth);
  
  &:hover {
    border-color: lighten(#2C5F4F, 20%);
  }
  
  &:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px rgba(44, 95, 79, 0.1);
  }
  
  &::placeholder {
    color: var(--color-subtle);
  }
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.conflict-alert {
  background: #FFF8E1;
  border: 2px solid var(--color-accent);
  border-radius: 10px;
  padding: 1.25rem;
  margin-top: 1.5rem;
  
  .alert-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    font-weight: 600;
    color: #E65100;
    margin-bottom: 1rem;
    
    i {
      font-size: 1.2rem;
    }
  }
  
  .conflict-list {
    background: white;
    border-radius: 6px;
    padding: 0.75rem;
    margin-bottom: 0.75rem;
  }
  
  .conflict-item {
    padding: 0.75rem;
    border-bottom: 1px solid var(--color-secondary);
    
    &:last-child {
      border-bottom: none;
    }
    
    .conflict-date {
      font-weight: 500;
      color: var(--color-text);
      margin-bottom: 0.25rem;
    }
    
    .conflict-client {
      font-size: 0.9rem;
      color: var(--color-subtle);
    }
  }
  
  .btn-link {
    background: none;
    border: none;
    color: var(--color-primary);
    font-weight: 500;
    cursor: pointer;
    text-decoration: underline;
    padding: 0;
    margin-bottom: 0.75rem;
    
    &:hover {
      color: darken(#2C5F4F, 12%);
    }
  }
  
  .alert-guidance {
    font-size: 0.9rem;
    color: #E65100;
    margin: 0;
    line-height: 1.5;
  }
}

@keyframes modalFade {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
```

---

## Interaction Patterns

### 1. Adding a Recurring Schedule
1. Admin selects therapist from left sidebar
2. Clicks "Configure Schedule" button
3. Right panel slides in with smooth animation
4. Admin checks days and sets time ranges
5. Real-time preview updates in calendar canvas (ghost overlay)
6. Click "Save Weekly Schedule" → confirmation toast → panel closes

### 2. Creating a Schedule Override
1. In config panel, click "+ Add Override" in Overrides section
2. Modal appears with date picker (calendar popup)
3. Toggle: "Unavailable" or "Custom Hours"
4. If custom hours: Time range pickers appear
5. Optional reason field
6. Submit → override card appears in list with smooth slide-in animation
7. Calendar canvas updates immediately with amber-bordered slot

### 3. Submitting Leave Request (Therapist)
1. Therapist clicks "Request Leave" button
2. Modal opens centered with backdrop blur
3. Fill form: type, dates, optional reason
4. As dates are selected, system checks for conflicts in real-time
5. If conflicts exist, warning box expands with animation, showing appointment details
6. Submit → request moves to "Pending" state in admin view
7. Therapist sees confirmation toast: "Leave request submitted successfully"
8. **Exception**: If therapist has Admin Staff role, leave is auto-approved (bypasses workflow)

### 3a. Therapist with Receptionist Role Modifying Own Schedule
1. Therapist (with Receptionist role) views their own schedule
2. "Configure Schedule" button is visible and enabled
3. Clicks button → config panel slides in (same as admin view)
4. Can modify recurring schedule, add overrides, and manage leave
5. Changes apply immediately without approval workflow
6. All modifications are scoped to their own schedule only (cannot see/edit other therapists)
7. Audit log records changes with therapist's user ID

### 4. Approving Leave (Admin)
1. Pending leave cards show pulsing indicator
2. Admin clicks "Approve" button on leave card
3. Confirmation dialog: "Approve leave for [dates]? This will block {{ conflictCount }} appointment slot(s)."
4. Confirm → leave status changes to "Approved"
5. Calendar canvas updates: affected dates turn solid terracotta
6. Smooth color transition animation (0.6s)
7. If conflicts exist, notification modal suggests next steps

---

## Responsive Behavior

### Desktop (>1200px)
- Full split-panel layout
- Week view with 7 days visible
- Config panel as right-side drawer (480px)

### Tablet (768px - 1200px)
- Sidebar collapses to icon bar (expandable on click)
- Calendar takes full width
- Config panel overlays calendar (full-width sheet from bottom)
- Week view compresses to 5 days with horizontal scroll

### Mobile (<768px)
- Hamburger menu for therapist list
- Single-day view by default (swipe to navigate days)
- Config options in full-screen modal
- Time slots stack vertically
- Touch-optimized targets (min 44px height)

---

## Accessibility (WCAG 2.1 AA)

### Keyboard Navigation
- Tab order: Header controls → Therapist list → Calendar grid → Config panel
- Arrow keys navigate calendar slots (up/down for time, left/right for days)
- Enter/Space activates slot interaction
- Escape closes modals and panels

### Screen Reader Support
- All slots have aria-label: "Monday 9:00 AM, Available" or "Wednesday 2:00 PM, Booked with Sarah Mitchell"
- Leave cards announce status: "Annual Leave, May 1 to May 14, Approved"
- Form inputs have explicit labels (not just placeholders)
- Modal focus trap: Focus moves to modal on open, returns to trigger on close

### Color & Contrast
- All text meets 4.5:1 contrast ratio minimum
- Status indicators use shape/pattern in addition to color:
  - Available: Dotted border pattern
  - Unavailable: Diagonal stripes
  - Leave: Solid with rounded corners
  - Override: Thick border + icon
- Focus indicators: 3px solid outline with adequate offset

### Motion & Animation
- Respect `prefers-reduced-motion` media query
- All animations can be disabled via CSS variable override
- No auto-playing carousels or infinite loops

---

## Technical Implementation Notes

### Angular Integration
- Use standalone components (no NgModules)
- Lazy-load with `loadComponent` in routes
- PrimeNG Calendar and Dropdown for base widgets, styled to match aesthetic
- Use Angular animations (`@angular/animations`) for state transitions
- RxJS for conflict detection (debounced API calls)

### State Management
- Component-level state for UI interactions
- Service layer (`ScheduleService`) for API integration
- Optimistic updates with rollback on error
- Real-time conflict detection via debounced API call (300ms)

### Security & Role-Based Authorization
- **Role Definitions**:
  - `ADMIN` or `SYSTEM_ADMINISTRATOR`: Full access to all therapists' schedules
  - `THERAPIST` + `RECEPTIONIST`: Can edit their own schedule without approval
  - `THERAPIST` + `ADMIN_STAFF`: Can manage own schedule + auto-approved leave
  - `THERAPIST` only: Read-only view + leave request submission
- **Frontend Guards**:
  - Check user roles from auth token/session
  - Conditionally render "Configure Schedule" button based on `RECEPTIONIST` role
  - Disable slot editing in UI if user lacks edit permissions
- **Backend Enforcement**:
  - API endpoints must verify user has appropriate role before accepting mutations
  - Therapist+Receptionist can only modify their own `therapistId` schedules
  - Audit log records all changes with user identity and role context

### Performance Considerations
- Virtual scrolling for therapist list if >50 therapists
- Calendar renders only visible time range (lazy-load slots on scroll)
- Memoize availability calculations
- Debounce conflict checks during date selection
- CSS animations only (no JS animation loops)

### API Integration Points
```typescript
// Service methods needed
ScheduleService {
  getTherapistSchedule(therapistId, startDate, endDate): Observable<Schedule>
  updateRecurringSchedule(therapistId, weeklyHours): Observable<void>
  createOverride(therapistId, override): Observable<Override>
  deleteOverride(overrideId): Observable<void>
  createLeaveRequest(therapistId, leave): Observable<LeaveRequest>
  approveLeave(leaveId): Observable<void>
  rejectLeave(leaveId, reason): Observable<void>
  checkConflicts(therapistId, startDate, endDate): Observable<Conflict[]>
}
```

---

## Visual Mockup Summary

### Key Differentiators
1. **Editorial Typography**: Cormorant Garamond brings sophistication, Work Sans ensures legibility
2. **Warm Earth Palette**: Moves away from typical cold blue/gray calendars
3. **Organic Transitions**: Smooth, purpose-driven animations feel natural, not mechanical
4. **Layered Information**: Card-based design creates visual hierarchy without clutter
5. **Contextual Indicators**: Status communicated through shape, pattern, color, and icon
6. **Generous Spacing**: Even when data-dense, breathing room maintains calm
7. **Staggered Reveals**: Calendar slots fade in with slight delay creating fluid, delightful load

### Emotional Impact
- **Admin Experience**: Confident, in-control, efficient — "I can manage complex schedules with ease"
- **Therapist Experience**: Empowered, informed, respected — "My schedule is transparent and I have agency"
- **Overall Brand**: Professional yet warm, sophisticated yet approachable

---

## Next Steps for Implementation

1. Create Angular standalone components structure in `frontend/src/app/features/schedule/`
2. Implement `ScheduleService` with API integration and role-based authorization checks
3. Build calendar grid with slot rendering logic
4. Implement role-based UI permissions (show/hide "Configure Schedule" based on user roles)
5. Add config panel with form validation
6. Implement leave request modal with conflict detection
7. Add approval workflow bypass logic for Admin Staff role
8. Add animations using Angular animations API
9. Integrate PrimeNG widgets (styled to match design system)
10. Write unit tests for schedule logic, conflict detection, and role-based permissions
11. Implement backend authorization guards for schedule mutation endpoints
12. Accessibility audit with axe DevTools
13. Performance testing with Lighthouse (target: >90 score)

---

## Design Assets Requirements

### Fonts
- **Cormorant Garamond**: 400, 600, 700 weights (Google Fonts)
- **Work Sans**: 300, 400, 500, 600 weights (Google Fonts)

### Icons
- PrimeIcons library (already in project)
- Custom SVG icons for leave types (can use emoji as fallback)

### Color Variables
All colors defined in `:root` CSS variables for easy theming and future dark mode support.

---

## Validation Checklist

- [ ] All time slots are keyboard navigable
- [ ] Screen reader announces slot status correctly
- [ ] Color contrast meets WCAG AA (4.5:1 minimum)
- [ ] Focus indicators are clearly visible
- [ ] Forms have proper label associations
- [ ] Error messages are announced to screen readers
- [ ] Touch targets are minimum 44x44px on mobile
- [ ] Animations respect `prefers-reduced-motion`
- [ ] No information conveyed by color alone
- [ ] Modal focus trap works correctly
