import { Directive, effect, inject, TemplateRef, ViewContainerRef } from '@angular/core';
import { PermissionService } from '../permission.service';
import { PermissionKey } from '../permissions.config';

/**
 * Structural directive that removes the host element from the DOM when the
 * current user does not hold the required permission.
 *
 * Usage:
 *   <button *appHasPermission="'VIEW_BILLING_ACTIONS'">Invoice</button>
 */
@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly permissionService = inject(PermissionService);

  private requiredKey: PermissionKey | null = null;
  private hasView = false;

  constructor() {
    // React to role changes automatically
    effect(() => {
      this.updateView();
    });
  }

  set appHasPermission(key: PermissionKey) {
    this.requiredKey = key;
    this.updateView();
  }

  private updateView(): void {
    const allowed = this.requiredKey !== null
      ? this.permissionService.hasPermission(this.requiredKey)
      : false;

    if (allowed && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!allowed && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
