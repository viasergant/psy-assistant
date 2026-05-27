import { Directive, effect, inject, Input, TemplateRef, ViewContainerRef } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AppMode, AppModeService } from './app-mode.service';

/**
 * Structural directive that conditionally renders a template based on the
 * current deployment mode.
 *
 * Usage:
 *   <section *appIfMode="'CLINIC'">Clinic-only content</section>
 *   <section *appIfMode="'SOLO'">Solo-only content</section>
 *
 * The element is removed from the DOM when the deployment mode does not match.
 * Re-evaluates automatically whenever AppModeService.mode$ emits.
 */
@Directive({
  selector: '[appIfMode]',
  standalone: true,
})
export class AppIfModeDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly modeService = inject(AppModeService);

  private readonly currentMode = toSignal(this.modeService.mode$, {
    initialValue: this.modeService.mode,
  });

  private requiredMode: AppMode | null = null;
  private hasView = false;

  constructor() {
    effect(() => {
      this.updateView(this.currentMode());
    });
  }

  @Input()
  set appIfMode(mode: AppMode) {
    this.requiredMode = mode;
    this.updateView(this.currentMode());
  }

  private updateView(currentMode: AppMode | null): void {
    const shouldShow = this.requiredMode !== null && currentMode === this.requiredMode;

    if (shouldShow && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!shouldShow && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
