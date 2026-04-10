import { Component, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Toolbar } from 'primeng/toolbar';
import { AuthService } from '../../core/auth/auth.service';
import { PermissionService } from '../../core/auth/permission.service';
import { NAV_ITEMS } from '../../core/auth/permissions.config';
import { GlobalSearchComponent } from '../global-search/global-search.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
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

  /** Nav items filtered to those the current user's role permits. */
  readonly visibleNav = computed(() =>
    NAV_ITEMS.filter(item => this.permissionService.hasAnyRole(item.roles))
  );

  onLogout(): void {
    this.authService.logout();
  }
}
