import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import {
  ScheduleSummary,
  DayOfWeek,
  getDayLabel,
  AvailabilitySlot,
  getDayOfWeek,
  Appointment
} from '../../models/schedule.model';
import { AvailabilityService } from '../../services/availability.service';
import { AppointmentApiService } from '../../services/appointment-api.service';
import { startOfWeek, addDays, format, isSameDay, parseISO } from 'date-fns';
import { forkJoin } from 'rxjs';
import { AppointmentBookingDialogComponent } from '../appointment-booking-dialog/appointment-booking-dialog.component';
import { AppointmentRescheduleDialogComponent } from '../appointment-reschedule-dialog/appointment-reschedule-dialog.component';
import { AppointmentCancelDialogComponent } from '../appointment-cancel-dialog/appointment-cancel-dialog.component';
import { AppointmentEditDialogComponent } from '../appointment-edit-dialog/appointment-edit-dialog.component';

interface WeekDay {
  date: Date;
  dateString: string;
  dayOfWeek: DayOfWeek;
  dayLabel: string;
}

interface TimeSlot {
  hour: number;
  minute: number;
  timeString: string;
  displayTime: string;
}

interface CalendarCell {
  day: WeekDay;
  timeSlot: TimeSlot;
  available: boolean;
  hasOverride: boolean;
  isLeave: boolean;
  leaveStatus?: 'PENDING' | 'APPROVED';
  isBooked: boolean;
  clientName?: string; // Client name for booked appointments
}

@Component({
  selector: 'app-schedule-calendar',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoPipe,
    AppointmentBookingDialogComponent,
    AppointmentRescheduleDialogComponent,
    AppointmentCancelDialogComponent,
    AppointmentEditDialogComponent
  ],
  templateUrl: './schedule-calendar.component.html',
  styleUrls: ['./schedule-calendar.component.scss']
})
export class ScheduleCalendarComponent implements OnInit {
  @Input() therapistProfileId!: string;
  @Input() schedule?: ScheduleSummary;
  @Input() editable = false;
  @Input() clients: Array<{id: string; name: string}> = []; // For booking dialog

  currentWeekStart: Date = startOfWeek(new Date(), { weekStartsOn: 1 });
  weekDays: WeekDay[] = [];
  timeSlots: TimeSlot[] = [];
  calendarCells: CalendarCell[][] = [];
  appointments: Appointment[] = [];
  loading = false;

  // Dialog state
  showBookingDialog = false;
  showRescheduleDialog = false;
  showEditDialog = false;
  showCancelDialog = false;
  selectedAppointment: Appointment | null = null;
  selectedDateTime: Date | null = null;
  
  // Appointment menu state
  showAppointmentMenu = false;
  appointmentMenuPosition = { x: 0, y: 0 };

  constructor(
    private availabilityService: AvailabilityService,
    private appointmentService: AppointmentApiService
  ) {}

  ngOnInit(): void {
    this.generateWeekDays();
    this.generateTimeSlots();
    this.loadAvailability();
  }

  /**
   * Generate array of days for current week (Monday-Sunday)
   */
  private generateWeekDays(): void {
    this.weekDays = [];
    for (let i = 0; i < 7; i++) {
      const date = addDays(this.currentWeekStart, i);
      const dateString = format(date, 'yyyy-MM-dd');
      const dayOfWeek = getDayOfWeek(date);
      const dayLabel = getDayLabel(dayOfWeek);

      this.weekDays.push({
        date,
        dateString,
        dayOfWeek,
        dayLabel
      });
    }
  }

  /**
   * Generate array of 30-minute time slots (6:00 AM - 10:00 PM)
   */
  private generateTimeSlots(): void {
    this.timeSlots = [];
    const startHour = 6;
    const endHour = 22;

    for (let hour = startHour; hour <= endHour; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeString = `${hour.toString().padStart(2, '0')}:${minute
          .toString()
          .padStart(2, '0')}`;
        const isPM = hour >= 12;
        const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
        const displayTime = `${displayHour}:${minute
          .toString()
          .padStart(2, '0')} ${isPM ? 'PM' : 'AM'}`;

        this.timeSlots.push({
          hour,
          minute,
          timeString,
          displayTime
        });
      }
    }
  }

  /**
   * Load availability data and appointments from backend
   * Public so it can be called by parent component after config changes
   */
  public loadAvailability(): void {
    if (!this.therapistProfileId) return;

    this.loading = true;
    const startDate = format(this.currentWeekStart, 'yyyy-MM-dd');
    const endDate = format(addDays(this.currentWeekStart, 6), 'yyyy-MM-dd');

    console.log(`Loading availability and appointments for therapist ${this.therapistProfileId} from ${startDate} to ${endDate}`);

    forkJoin({
      slots: this.availabilityService.getAvailableSlots(this.therapistProfileId, startDate, endDate),
      appointments: this.appointmentService.getTherapistAppointments(this.therapistProfileId, startDate, endDate)
    }).subscribe({
      next: ({ slots, appointments }) => {
        console.log(`Received ${slots.length} available slots and ${appointments.length} appointments`);
        this.appointments = appointments;
        this.buildCalendarGrid(slots);
        this.loading = false;
      },
      error: err => {
        console.error('Error loading availability and appointments:', err);
        this.loading = false;
      }
    });
  }

  /**
   * Build 2D grid of calendar cells
   */
  private buildCalendarGrid(availabilitySlots: AvailabilitySlot[]): void {
    this.calendarCells = [];

    for (const timeSlot of this.timeSlots) {
      const row: CalendarCell[] = [];

      for (const day of this.weekDays) {
        // Normalize time format: backend returns "HH:mm:ss", frontend uses "HH:mm"
        const slot = availabilitySlots.find(
          s =>
            s.date === day.dateString &&
            s.startTime.substring(0, 5) === timeSlot.timeString
        );

        const isLeave = this.getLeaveStatusForDate(day.date) !== null;
        const leaveStatus = this.getLeaveStatusForDate(day.date);
        const hasOverride = this.hasOverrideForDate(day.date);
        const available = slot?.available ?? false;

        // Check if this time slot has an appointment
        const isBooked = this.isSlotBooked(day.date, timeSlot.hour, timeSlot.minute);
        
        // Get client name for booked slots
        let clientName: string | undefined;
        if (isBooked) {
          const appointment = this.getAppointmentForSlot(day.date, timeSlot.hour, timeSlot.minute);
          if (appointment) {
            const client = this.clients.find(c => c.id === appointment.clientId);
            clientName = client?.name;
          }
        }

        row.push({
          day,
          timeSlot,
          available,
          hasOverride,
          isLeave,
          leaveStatus: leaveStatus || undefined,
          isBooked,
          clientName
        });
      }

      this.calendarCells.push(row);
    }
  }

  /**
   * Check if date falls within a leave period and return status
   */
  private getLeaveStatusForDate(date: Date): 'PENDING' | 'APPROVED' | null {
    if (!this.schedule?.leavePeriods) return null;

    for (const leave of this.schedule.leavePeriods) {
      // Only show PENDING and APPROVED leave (not REJECTED or CANCELLED)
      if (leave.status !== 'PENDING' && leave.status !== 'APPROVED') {
        continue;
      }

      const start = parseISO(leave.startDate);
      const end = parseISO(leave.endDate);
      if (date >= start && date <= end) {
        return leave.status === 'APPROVED' ? 'APPROVED' : 'PENDING';
      }
    }

    return null;
  }

  /**
   * Check if date has a schedule override
   */
  private hasOverrideForDate(date: Date): boolean {
    if (!this.schedule?.overrides) return false;

    return this.schedule.overrides.some(override =>
      isSameDay(parseISO(override.date), date)
    );
  }

  /**
   * Check if a specific time slot has an appointment
   */
  private isSlotBooked(date: Date, hour: number, minute: number): boolean {
    if (!this.appointments || this.appointments.length === 0) return false;

    return this.appointments.some(appointment => {
      const appointmentStart = parseISO(appointment.startTime);
      const appointmentEnd = parseISO(appointment.endTime);
      
      // Create a date-time for the slot
      const slotDateTime = new Date(date);
      slotDateTime.setHours(hour, minute, 0, 0);

      // Check if slot falls within appointment time range (inclusive start, exclusive end)
      return slotDateTime >= appointmentStart && slotDateTime < appointmentEnd;
    });
  }

  /**
   * Get the appointment for a specific time slot
   */
  private getAppointmentForSlot(date: Date, hour: number, minute: number): Appointment | undefined {
    if (!this.appointments || this.appointments.length === 0) return undefined;

    return this.appointments.find(appointment => {
      const appointmentStart = parseISO(appointment.startTime);
      const appointmentEnd = parseISO(appointment.endTime);
      
      // Create a date-time for the slot
      const slotDateTime = new Date(date);
      slotDateTime.setHours(hour, minute, 0, 0);
      
      // Check if slot falls within appointment time range (inclusive start, exclusive end)
      return slotDateTime >= appointmentStart && slotDateTime < appointmentEnd;
    });
  }

  /**
   * Navigate to previous week
   */
  previousWeek(): void {
    this.currentWeekStart = addDays(this.currentWeekStart, -7);
    this.generateWeekDays();
    this.loadAvailability();
  }

  /**
   * Navigate to next week
   */
  nextWeek(): void {
    this.currentWeekStart = addDays(this.currentWeekStart, 7);
    this.generateWeekDays();
    this.loadAvailability();
  }

  /**
   * Jump to current week
   */
  goToToday(): void {
    this.currentWeekStart = startOfWeek(new Date(), { weekStartsOn: 1 });
    this.generateWeekDays();
    this.loadAvailability();
  }

  /**
   * Get formatted week range display
   */
  getWeekRangeDisplay(): string {
    const start = format(this.currentWeekStart, 'MMM d');
    const end = format(addDays(this.currentWeekStart, 6), 'MMM d, yyyy');
    return `${start} - ${end}`;
  }

  /**
   * Get CSS class for calendar cell
   */
  getCellClass(cell: CalendarCell): string {
    if (cell.isLeave) {
      return cell.leaveStatus === 'PENDING' ? 'cell-leave-pending' : 'cell-leave';
    }
    if (cell.isBooked) return 'cell-booked';
    if (cell.hasOverride) return 'cell-override';
    if (cell.available) return 'cell-available';
    return 'cell-unavailable';
  }

  /**
   * Get tooltip text for calendar cell
   */
  getCellTitle(cell: CalendarCell): string {
    if (cell.isLeave) {
      return cell.leaveStatus === 'PENDING' ? 'Leave request pending approval' : 'On leave (approved)';
    }
    if (cell.isBooked) {
      return cell.clientName ? `Appointment: ${cell.clientName}` : 'Appointment booked';
    }
    if (cell.hasOverride) return 'Schedule override';
    if (cell.available) return 'Available';
    return 'Unavailable';
  }

  /**
   * Get time range display for a cell (start - end time) in 24h format
   */
  getTimeRange(cell: CalendarCell): string {
    const startHour = cell.timeSlot.hour;
    const startMinute = cell.timeSlot.minute;
    
    // Calculate end time (30 minutes later)
    let endHour = startHour;
    let endMinute = startMinute + 30;
    if (endMinute >= 60) {
      endMinute = 0;
      endHour++;
    }
    
    // Format times in 24h format
    const formatTime = (hour: number, minute: number): string => {
      return `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
    };
    
    return `${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}`;
  }

  /**
   * Handle cell click
   */
  onCellClick(cell: CalendarCell, event: MouseEvent): void {
    if (!this.editable) return;
    if (cell.isBooked) {
      // Show appointment menu with Reschedule and Cancel options
      const appointment = this.getAppointmentForSlot(cell.day.date, cell.timeSlot.hour, cell.timeSlot.minute);
      if (appointment) {
        this.selectedAppointment = appointment;
        this.appointmentMenuPosition = { x: event.clientX, y: event.clientY };
        this.showAppointmentMenu = true;
        event.stopPropagation();
      }
    } else if (cell.available) {
      // Open booking dialog for this time slot with prefilled date/time
      const dateTime = this.createDateTimeFromCell(cell);
      this.openBookingDialog(dateTime);
    }
  }
  
  /**
   * Handle reschedule action from menu
   */
  onRescheduleMenuAction(): void {
    if (this.selectedAppointment) {
      this.showAppointmentMenu = false;
      this.openRescheduleDialog(this.selectedAppointment);
    }
  }

  /**
   * Handle edit action from menu
   */
  onEditMenuAction(): void {
    if (this.selectedAppointment) {
      this.showAppointmentMenu = false;
      this.openEditDialog(this.selectedAppointment);
    }
  }
  
  /**
   * Handle cancel action from menu
   */
  onCancelMenuAction(): void {
    if (this.selectedAppointment) {
      this.showAppointmentMenu = false;
      this.openCancelDialog(this.selectedAppointment);
    }
  }
  
  /**
   * Close appointment menu
   */
  closeAppointmentMenu(): void {
    this.showAppointmentMenu = false;
    this.selectedAppointment = null;
  }

  /**
   * Create Date object from calendar cell
   */
  private createDateTimeFromCell(cell: CalendarCell): Date {
    const date = new Date(cell.day.date);
    date.setHours(cell.timeSlot.hour, cell.timeSlot.minute, 0, 0);
    return date;
  }

  /**
   * Open booking dialog with optional prefilled date/time
   */
  openBookingDialog(dateTime?: Date): void {
    this.selectedDateTime = dateTime || null;
    this.showBookingDialog = true;
  }

  /**
   * Handle booking submission
   */
  onBookingSubmitted(appointment: Appointment): void {
    this.showBookingDialog = false;
    this.selectedDateTime = null;
    console.log('Appointment booked:', appointment);
    // Reload calendar to show new appointment
    this.loadAvailability();
    // TODO: Emit event to parent component
  }

  /**
   * Handle booking cancellation
   */
  onBookingCancelled(): void {
    this.showBookingDialog = false;
    this.selectedDateTime = null;
  }

  // ========== Appointment Reschedule Dialog ==========

  /**
   * Open reschedule dialog for an appointment
   */
  openRescheduleDialog(appointment: Appointment): void {
    this.selectedAppointment = appointment;
    this.showRescheduleDialog = true;
  }

  /**
   * Handle reschedule submission
   */
  onRescheduleSubmitted(appointment: Appointment): void {
    this.showRescheduleDialog = false;
    this.selectedAppointment = null;
    console.log('Appointment rescheduled:', appointment);
    // Reload calendar to show updated appointment
    this.loadAvailability();
    // TODO: Emit event to parent component
  }

  /**
   * Handle reschedule cancellation
   */
  onRescheduleCancelled(): void {
    this.showRescheduleDialog = false;
    this.selectedAppointment = null;
  }

  // ========== Appointment Cancel Dialog ==========

  /**
   * Open cancel dialog for an appointment
   */
  openCancelDialog(appointment: Appointment): void {
    this.selectedAppointment = appointment;
    this.showCancelDialog = true;
  }

  /**
   * Handle cancellation submission
   */
  onCancelSubmitted(appointment: Appointment): void {
    this.showCancelDialog = false;
    this.selectedAppointment = null;
    console.log('Appointment cancelled:', appointment);
    // Reload calendar to show updated appointment as cancelled
    this.loadAvailability();
    // TODO: Emit event to parent component
  }

  /**
   * Handle cancellation dialog close
   */
  onCancelDialogCancelled(): void {
    this.showCancelDialog = false;
    this.selectedAppointment = null;
  }

  // ========== Appointment Edit Dialog ==========

  /**
   * Open edit dialog for an appointment
   */
  openEditDialog(appointment: Appointment): void {
    this.selectedAppointment = appointment;
    this.showEditDialog = true;
  }

  /**
   * Handle edit submission
   */
  onEditSubmitted(appointment: Appointment): void {
    this.showEditDialog = false;
    this.selectedAppointment = null;
    console.log('Appointment updated:', appointment);
    // Reload calendar to show updated appointment
    this.loadAvailability();
    // TODO: Emit event to parent component
  }

  /**
   * Handle edit dialog close
   */
  onEditCancelled(): void {
    this.showEditDialog = false;
    this.selectedAppointment = null;
  }
}
