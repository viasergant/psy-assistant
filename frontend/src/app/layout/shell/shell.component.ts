import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Toolbar } from 'primeng/toolbar';
import { AuthService } from '../../core/auth/auth.service';
import { PermissionService } from '../../core/auth/permission.service';
import { NAV_ITEMS } from '../../core/auth/permissions.config';
import { AppModeService } from '../../core/mode/app-mode.service';
import { GlobalSearchComponent } from '../global-search/global-search.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    NgClass,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    TranslocoPipe,
    Toolbar,
    GlobalSearchComponent
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly permissionService = inject(PermissionService);
  private readonly modeService = inject(AppModeService);

  /** Reactive signal over the deployment mode so computed() re-evaluates on mode changes. */
  private readonly currentMode = toSignal(this.modeService.mode$, {
    initialValue: this.modeService.mode,
  });

  /**
   * Nav items filtered to those the current user's role permits and that are
   * visible in the current deployment mode. Clinic-only items (clinicOnly: true)
   * are hidden unless the deployment mode is CLINIC.
   */
  readonly visibleNav = computed(() => {
    const mode = this.currentMode();
    return NAV_ITEMS.filter(item =>
      this.permissionService.hasAnyRole(item.roles) &&
      (!item.clinicOnly || mode === 'CLINIC')
    );
  });

  onLogout(): void {
    this.authService.logout();
  }
}
