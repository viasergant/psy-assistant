# Shared Styles Module

Comprehensive shared styles system for consistent UI patterns across the PSY-ASSISTANT application.

## Overview

This module provides:
- **Design tokens** (spacing, colors, shadows, radii) aligned with `docs/ui-design-system.md`
- **Component styles** (dialogs, forms, buttons, badges, tables)
- **Utility classes** (layout, spacing, typography)
- **Reusable SCSS mixins** for custom components

**Location:** `frontend/src/styles/`

## Quick Start

### Using Dialog Styles

```html
<!-- Dialog overlay and container -->
<div class="dialog-overlay">
  <div class="dialog">
    <div class="dialog-header">
      <h2>Edit Therapist</h2>
      <button class="dialog-close" (click)="close()">✕</button>
    </div>
    
    <div class="dialog-content">
      <!-- Your content here -->
    </div>
    
    <div class="dialog-actions">
      <button type="button" class="btn-secondary" (click)="cancel()">Cancel</button>
      <button type="submit" class="btn-primary">Save</button>
    </div>
  </div>
</div>
```

### Using Form Styles

```html
<form>
  <div class="field">
    <label>Full Name <span class="required">*</span></label>
    <input type="text" name="fullName" placeholder="Enter full name" />
    <span class="error-msg" *ngIf="errors.fullName">{{ errors.fullName }}</span>
  </div>
  
  <div class="field-row">
    <div class="field">
      <label>Email</label>
      <input type="email" name="email" />
    </div>
    <div class="field">
      <label>Phone</label>
      <input type="tel" name="phone" />
    </div>
  </div>
</form>
```

### Using Button Styles

```html
<!-- Primary action -->
<button type="submit" class="btn-primary">Save Changes</button>

<!-- Secondary action -->
<button type="button" class="btn-secondary">Cancel</button>

<!-- Destructive action -->
<button class="btn-danger" (click)="delete()">Delete</button>

<!-- Ghost/text button -->
<button class="btn-ghost">Learn More</button>

<!-- Small variant -->
<button class="btn-primary btn-sm">Small Button</button>

<!-- Loading state -->
<button class="btn-primary btn-loading">Saving...</button>
```

### Using Badges

```html
<!-- Status badges -->
<span class="badge badge-active">Active</span>
<span class="badge badge-inactive">Inactive</span>
<span class="badge badge-warning">Pending</span>
<span class="badge badge-error">Failed</span>
<span class="badge badge-info">New</span>

<!-- With dot indicator -->
<span class="badge badge-success badge-dot">Online</span>

<!-- Pill variant -->
<span class="badge badge-primary badge-pill">Featured</span>
```

### Using Table Styles

```html
<div class="table-wrapper table-hoverable">
  <table>
    <thead>
      <tr>
        <th>Name</th>
        <th>Email</th>
        <th>Status</th>
        <th class="table-actions">Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>John Doe</td>
        <td>john@example.com</td>
        <td><span class="badge badge-active">Active</span></td>
        <td class="table-actions">
          <button class="btn-table-action" title="Edit">✏️</button>
          <button class="btn-table-action" title="Delete">🗑️</button>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

## Design Tokens Reference

### Spacing Scale (8px base grid)

Use spacing tokens via CSS variables or utility classes:

| Token | Value | Usage |
|-------|-------|-------|
| `--spacing-xs` | 4px | Tight spacing, icon gaps |
| `--spacing-sm` | 8px | Small gaps, compact layouts |
| `--spacing-md` | 16px | Default spacing between elements |
| `--spacing-lg` | 24px | Generous spacing, field margins |
| `--spacing-xl` | 32px | Section padding, card padding |
| `--spacing-2xl` | 48px | Large section gaps |
| `--spacing-3xl` | 64px | Hero section padding |

**Example:**
```css
.my-component {
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
}
```

### Colors

| Token | Value | Usage |
|-------|-------|-------|
| `--color-accent` | #0EA5A0 | Primary actions, links |
| `--color-accent-hover` | #0C9490 | Hover state for accent |
| `--color-text-primary` | #0F172A | Main body text |
| `--color-text-secondary` | #64748B | Secondary text, labels |
| `--color-text-muted` | #94A3B8 | Placeholder text, hints |
| `--color-error` | #DC2626 | Error messages, destructive actions |
| `--color-border` | #E2E8F0 | Borders, dividers |
| `--color-surface` | #FFFFFF | Cards, surfaces, inputs |
| `--color-bg` | #F5F4F1 | Page background |

### Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-sm` | 4px | Badges, small elements |
| `--radius-md` | 8px | Buttons, inputs, cards |
| `--radius-lg` | 12px | Dialogs, large containers |

### Shadows

| Token | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,.05)` | Subtle elevation |
| `--shadow-md` | Complex | Cards, dropdowns |
| `--shadow-lg` | Complex | Dialogs, popovers |

## Component Styles

### Dialogs

| Class | Description |
|-------|-------------|
| `.dialog-overlay` | Full-screen backdrop with blur |
| `.dialog` | Dialog container (default 540px width) |
| `.dialog-sm` | Small dialog (480px) |
| `.dialog-lg` | Large dialog (640px) |
| `.dialog-header` | Header with title and close button |
| `.dialog-close` | Close button styling |
| `.dialog-content` | Main content area |
| `.dialog-actions` | Footer with action buttons |

**Mixin:**
```scss
@include dialog-base($width: 600px, $max-height: 85vh);
```

### Forms

| Class | Description |
|-------|-------------|
| `.field` | Form field container with vertical layout |
| `.field-row` | Horizontal layout for multiple fields |
| `.field-label` | Field label styling |
| `.form-input` | Apply to inputs not in `.field` |
| `.form-select` | Apply to selects not in `.field` |
| `.form-textarea` | Apply to textareas not in `.field` |
| `.field-error` / `.has-error` | Error state for field |
| `.error-msg` | Error message text |
| `.field-hint` | Helper text below field |
| `.required` | Required indicator (*) |

**Mixins:**
```scss
@include input-base;
@include input-focus;
@include input-error;
```

### Buttons

| Class | Description |
|-------|-------------|
| `.btn-primary` | Primary action button (teal) |
| `.btn-secondary` | Secondary action (white with border) |
| `.btn-danger` | Destructive action (red) |
| `.btn-ghost` | Text-only button (transparent) |
| `.btn-sm` | Small button |
| `.btn-lg` | Large button |
| `.btn-icon` | Icon-only button |
| `.btn-table-action` | Icon button for table rows |
| `.btn-loading` | Loading state with spinner |
| `.btn-group` | Button group container |

**Mixins:**
```scss
@include button-base;
@include button-states($bg-color, $hover-color, $shadow-color);
```

### Badges

| Class | Description |
|-------|-------------|
| `.badge` | Base badge |
| `.badge-success` / `.badge-active` | Green badge |
| `.badge-info` / `.badge-primary` | Blue badge |
| `.badge-warning` | Amber badge |
| `.badge-error` / `.badge-danger` | Red badge |
| `.badge-inactive` / `.badge-neutral` | Gray badge |
| `.badge-accent` | Teal badge |
| `.badge-sm` | Small badge |
| `.badge-lg` | Large badge |
| `.badge-dot` | Badge with status dot |
| `.badge-pill` | Fully rounded badge |
| `.badge-outline` | Outlined variant |

**Mixin:**
```scss
@include badge-variant($bg, $color);
```

### Tables

| Class | Description |
|-------|-------------|
| `.table-wrapper` | Table container with border and shadow |
| `.table-hoverable` | Enable row hover effects |
| `.table-striped` | Alternating row colors |
| `.table-compact` | Reduced padding |
| `.table-responsive` | Horizontal scroll on mobile |
| `.table-actions` | Right-aligned actions column |
| `.table-empty` | Empty state container |
| `.table-cell-nowrap` | Prevent text wrapping |
| `.table-cell-truncate` | Truncate with ellipsis |
| `.table-cell-numeric` | Right-aligned numbers |

## Utility Classes

### Layout

**Flexbox:**
- `.flex`, `.flex-inline` - Display flex
- `.flex-row`, `.flex-column` - Direction
- `.flex-center` - Center items both axes
- `.flex-between` - Space between
- `.items-center`, `.items-start`, `.items-end` - Align items
- `.flex-1`, `.flex-auto`, `.flex-none` - Flex grow/shrink

**Grid:**
- `.grid` - Display grid
- `.grid-cols-1` through `.grid-cols-4` - Column count
- `.grid-cols-auto` - Auto-fit columns (min 250px)

**Gap:**
- `.gap-sm`, `.gap-md`, `.gap-lg`, `.gap-xl` - Gap using spacing scale
- `.gap-x-md`, `.gap-y-lg` - Horizontal/vertical gap

**Container:**
- `.container` - Max-width 1200px with padding
- `.container-narrow` - Max-width 800px
- `.container-fluid` - Full width with padding

### Spacing

**Margin:** `.m-{size}`, `.mx-{size}`, `.my-{size}`, `.mt-{size}`, `.mr-{size}`, `.mb-{size}`, `.ml-{size}`
**Padding:** `.p-{size}`, `.px-{size}`, `.py-{size}`, `.pt-{size}`, `.pr-{size}`, `.pb-{size}`, `.pl-{size}`

**Sizes:** `0`, `xs`, `sm`, `md`, `lg`, `xl`, `2xl`, `3xl`, `auto` (margin only)

**Examples:**
```html
<div class="mb-lg px-xl">Content with bottom margin and horizontal padding</div>
<div class="mx-auto">Centered with auto margins</div>
```

### Typography

**Size:** `.text-xs`, `.text-sm`, `.text-base`, `.text-lg`, `.text-xl`, `.text-2xl`, `.text-3xl`, `.text-4xl`

**Weight:** `.font-normal`, `.font-medium`, `.font-semibold`, `.font-bold`

**Color:** `.text-primary`, `.text-secondary`, `.text-muted`, `.text-accent`, `.text-error`

**Alignment:** `.text-left`, `.text-center`, `.text-right`, `.text-justify`

**Transform:** `.uppercase`, `.lowercase`, `.capitalize`, `.normal-case`

**Decoration:** `.underline`, `.line-through`, `.no-underline`

**Overflow:** `.truncate`, `.text-ellipsis`, `.whitespace-nowrap`

## Migration Guide

### Converting Inline Styles to Shared Classes

**Before (inline styles in component):**
```typescript
@Component({
  selector: 'app-example-dialog',
  styles: [`
    .overlay {
      position: fixed; inset: 0;
      background: rgba(15, 23, 42, 0.6);
      ...
    }
    .dialog {
      background: #fff;
      padding: 2rem;
      ...
    }
  `]
})
```

**After (using shared classes):**
```html
<!-- In template -->
<div class="dialog-overlay">
  <div class="dialog">
    <!-- content -->
  </div>
</div>
```

### Using SCSS Mixins

Create a component `.scss` file:

```scss
// my-component.component.scss
@import '../../../styles/components/dialogs';

.custom-dialog {
  @include dialog-base(700px, 95vh);
  
  // Add component-specific styles
  .special-header {
    background: var(--color-accent);
    color: white;
  }
}
```

## Best Practices

### DO:
✅ Use spacing tokens (`var(--spacing-lg)`) instead of hardcoded px values
✅ Apply utility classes for common patterns (margins, flex, text colors)
✅ Use component classes (`.dialog`, `.field`, `.btn-primary`) for standard UI
✅ Reference design system colors via CSS variables (`var(--color-accent)`)
✅ Use mixins for customized variants that need base styles + overrides

### DON'T:
❌ Create new dialog/button/form styles in component files
❌ Hardcode colors, spacing, or border-radius values
❌ Duplicate table/badge patterns - use shared classes instead
❌ Override shared class styles with `!important` (extend via SCSS instead)
❌ Use inline `style=""` attributes (use utility classes)

## Examples

### Complex Form with Validation

```html
<form>
  <div class="field-row">
    <div class="field">
      <label>First Name <span class="required">*</span></label>
      <input type="text" [(ngModel)]="model.firstName" name="firstName" />
    </div>
    <div class="field">
      <label>Last Name <span class="required">*</span></label>
      <input type="text" [(ngModel)]="model.lastName" name="lastName" />
    </div>
  </div>
  
  <div class="field" [class.has-error]="errors.email">
    <label>Email Address</label>
    <input type="email" [(ngModel)]="model.email" name="email" />
    <span class="error-msg" *ngIf="errors.email">{{ errors.email }}</span>
    <span class="field-hint" *ngIf="!errors.email">We'll never share your email</span>
  </div>
  
  <div class="dialog-actions">
    <button type="button" class="btn-secondary" (click)="cancel()">Cancel</button>
    <button type="submit" class="btn-primary" [disabled]="!isValid">Create</button>
  </div>
</form>
```

### Data Table with Actions

```html
<div class="table-wrapper table-hoverable">
  <table>
    <thead>
      <tr>
        <th>Therapist</th>
        <th>Specialization</th>
        <th>Status</th>
        <th class="table-actions">Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let therapist of therapists" (click)="viewDetails(therapist)">
        <td class="font-medium">{{ therapist.fullName }}</td>
        <td class="text-secondary">{{ therapist.specialization }}</td>
        <td>
          <span class="badge" 
                [class.badge-active]="therapist.status === 'ACTIVE'"
                [class.badge-inactive]="therapist.status === 'INACTIVE'">
            {{ therapist.status }}
          </span>
        </td>
        <td class="table-actions">
          <button class="btn-table-action" (click)="edit($event, therapist)" title="Edit">
            <i class="pi pi-pencil"></i>
          </button>
          <button class="btn-table-action" (click)="delete($event, therapist)" title="Delete">
            <i class="pi pi-trash"></i>
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</div>

<div class="table-empty" *ngIf="therapists.length === 0">
  <div class="empty-icon">👥</div>
  <div class="empty-message">No therapists found</div>
  <div class="empty-hint">Click "Add Therapist" to create one</div>
</div>
```

## Troubleshooting

**Q: My dialog isn't animating**
A: Ensure you're using `.dialog-overlay` as the parent. The animation is triggered on mount.

**Q: Form styling isn't applied**
A: Wrap your input in a `.field` container or add the `.form-input` class directly to the input.

**Q: Button doesn't have hover effect**
A: Check that you're using one of the button classes (`.btn-primary`, `.btn-secondary`, etc.). The `[type="submit"]` selector also defaults to primary styling.

**Q: Table rows aren't hoverable**
A: Add `.table-hoverable` class to `.table-wrapper`.

**Q: Utility classes not working**
A: Verify that `styles/_index.scss` is imported in `src/styles.scss`.

## File Structure

```
frontend/src/styles/
├── _index.scss                 # Main entry point
├── base/
│   └── _animations.scss        # Keyframe animations
├── components/
│   ├── _dialogs.scss          # Dialog/modal components
│   ├── _forms.scss            # Form fields and inputs
│   ├── _buttons.scss          # Button variants
│   ├── _badges.scss           # Status badges
│   └── _tables.scss           # Data table styling
└── utilities/
    ├── _layout.scss           # Flex, grid, containers
    ├── _spacing.scss          # Margin/padding utilities
    └── _typography.scss       # Text utilities
```

## Related Documentation

- **Design System:** `frontend/docs/ui-design-system.md` - Full design specifications
- **i18n Structure:** `frontend/docs/i18n-key-structure.md` - Internationalization patterns
- **Project Guidelines:** `AGENTS.md` - Coding conventions and architectural decisions
