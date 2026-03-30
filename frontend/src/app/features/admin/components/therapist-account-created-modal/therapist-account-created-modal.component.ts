import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { UserCreationResponse } from '../../users/models/user.model';

/**
 * Modal dialog displayed after successfully creating a therapist account.
 * Shows the auto-generated temporary password that the admin must copy and share with the therapist.
 */
@Component({
  selector: 'app-therapist-account-created-modal',
  standalone: true,
  imports: [
    CommonModule,
    DialogModule,
    ButtonModule,
    MessageModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './therapist-account-created-modal.component.html',
  styleUrl: './therapist-account-created-modal.component.scss'
})
export class TherapistAccountCreatedModalComponent {
  @Input() visible = false;
  @Input() userData: UserCreationResponse | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() viewProfile = new EventEmitter<string>();

  constructor(private messageService: MessageService) {}

  /**
   * Copies both email and password to clipboard.
   */
  copyCredentials(): void {
    if (!this.userData) return;

    const credentials = `Email: ${this.userData.email}\nPassword: ${this.userData.temporaryPassword}`;
    this.copyToClipboard(credentials, 'Credentials copied to clipboard');
  }

  /**
   * Copies only the password to clipboard.
   */
  copyPassword(): void {
    if (!this.userData) return;

    this.copyToClipboard(this.userData.temporaryPassword, 'Password copied to clipboard');
  }

  /**
   * Helper method to copy text to clipboard and show success message.
   */
  private copyToClipboard(text: string, successMessage: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.messageService.add({
        severity: 'success',
        summary: 'Success',
        detail: successMessage,
        life: 3000
      });
    }).catch(err => {
      console.error('Failed to copy:', err);
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to copy to clipboard',
        life: 3000
      });
    });
  }

  /**
   * Closes the modal.
   */
  close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /**
   * Emits event to navigate to the therapist's profile.
   */
  onViewProfile(): void {
    if (this.userData) {
      this.viewProfile.emit(this.userData.id);
    }
    this.close();
  }
}
