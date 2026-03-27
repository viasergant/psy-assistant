# PSY Assistant CRM — UI Design System

> Living specification document. Update whenever design tokens or component patterns change in `src/styles.scss` or shared components.

---

## 1. Principles

| Principle | Description |
|---|---|
| **Restrained professionalism** | Clinical clarity over decorative detail. Every element serves a function. |
| **Accessible contrast** | All text/background pairs target WCAG AA (4.5:1 for body text, 3:1 for large text and UI components). |
| **Consistent surfaces** | Three surface levels: page background → card/panel → overlay/dialog. |
| **Token-first** | All colors, radii, and shadows are referenced via CSS custom properties, never hardcoded in component styles. |

---

## 2. Color Palette

### 2.1 Semantic tokens (CSS custom properties)

All tokens are declared on `:root` in `src/styles.scss`.

#### Page & Surface

| Token | Value | Usage |
|---|---|---|
| `--color-bg` | `#F5F4F1` | Page / app background |
| `--color-surface` | `#FFFFFF` | Cards, dialogs, toolbar, inputs |

#### Sidebar

| Token | Value | Usage |
|---|---|---|
| `--color-sidebar-bg` | `#141E2B` | Sidebar background (dark ink) |
| `--color-sidebar-hover` | `#1D2D40` | Nav item hover state |
| `--color-sidebar-active` | `#0EA5A0` | Active nav item background |
| `--color-sidebar-active-hover` | `#0C9490` | Active nav item hover |
| `--color-sidebar-text` | `#8DA0B3` | Idle nav link text |
| `--color-sidebar-text-active` | `#FFFFFF` | Active nav link text |

#### Accent (Primary interactive)

| Token | Value | Usage |
|---|---|---|
| `--color-accent` | `#0EA5A0` | Primary buttons, active states, focus rings, accents |
| `--color-accent-hover` | `#0C9490` | Primary button hover |
| `--color-accent-active` | `#0A8480` | Primary button active/pressed |

> **Contrast check:** `#FFFFFF` text on `#0EA5A0` ≈ 3.1:1 (passes AA for large text / UI components). For body text inside teal surfaces, use white at ≥ 18px bold or consider `#0A8480` as the surface.

#### Text

| Token | Value | Usage |
|---|---|---|
| `--color-text-primary` | `#0F172A` | Body text, headings, input values |
| `--color-text-secondary` | `#64748B` | Subtitles, helper text, meta |
| `--color-text-muted` | `#94A3B8` | Placeholders, disabled labels, timestamps |

> **Contrast on `#FFFFFF`:**
> - `#0F172A` → ~18:1 ✅
> - `#64748B` → ~5.5:1 ✅
> - `#94A3B8` → ~3.0:1 — suitable for placeholder/muted only; do not use for interactive text

#### Border

| Token | Value | Usage |
|---|---|---|
| `--color-border` | `#E2E8F0` | Input borders (default), table dividers, card borders, toolbar bottom |

#### Semantic states

| Token | Value | Usage |
|---|---|---|
| `--color-error` | `#DC2626` | Error text, error border, danger button bg |
| `--color-error-bg` | `#FEF2F2` | Error alert / input error background |
| `--color-error-border` | `#FECACA` | Error alert border |

**Success** (component-level only, not yet tokenised):

| Value | Usage |
|---|---|
| `#F0FDF4` | Success alert background |
| `#86EFAC` | Success alert border |
| `#166534` | Success alert text |

> Consider promoting these to tokens (`--color-success`, `--color-success-bg`, `--color-success-border`) when more success states are needed.

---

## 3. Typography

### 3.1 Font

| Token | Value |
|---|---|
| `--font-family` | `'Plus Jakarta Sans', system-ui, -apple-system, sans-serif` |

Loaded via Google Fonts in `src/index.html`:
```html
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
```

### 3.2 Scale

| Role | Size | Weight | Color token | Notes |
|---|---|---|---|---|
| Base | `15px` | 400 | `--color-text-primary` | Set on `html, body` |
| Page heading `h1` | `1.75rem` (≈26px) | 700 | `--color-text-primary` | Letter-spacing `-0.025em` |
| Section heading `h2` | `1.5rem` (≈22px) | 700 | `--color-text-primary` | |
| Dialog heading | `1.25rem` (≈19px) | 700 | `--color-text-primary` | |
| Table heading / label | `0.8125rem` (≈12px) | 500–600 | `#374151` | Used in filter labels, column headers |
| Body / input | `0.9375rem` (≈14px) | 400 | `--color-text-primary` | |
| Helper / muted | `0.875rem` (≈13px) | 400 | `--color-text-secondary` | |
| Error message | `0.8125rem` (≈12px) | 400 | `--color-error` | |
| Badge | `0.8rem` (≈12px) | 500 | varies | See badge section |

---

## 4. Spacing

Not yet tokenised — used consistently at these values:

| Scale | Value | Common use |
|---|---|---|
| 2xs | `0.25rem` (4px) | Gap between label and error message |
| xs | `0.375rem` (6px) | Gap between label and input |
| sm | `0.5rem` (8px) | Button vertical padding (secondary) |
| md | `0.75rem` (12px) | Alert padding, table cell padding |
| lg | `1rem` (16px) | Form field margin-bottom |
| xl | `1.25rem` (20px) | Button horizontal padding, sidebar item padding |
| 2xl | `1.5rem` (24px) | Section/page top margin, dialog padding |
| 3xl | `2rem` (32px) | Page padding, dialog padding |

---

## 5. Border Radius

| Token | Value | Usage |
|---|---|---|
| `--radius-sm` | `4px` | Badges |
| `--radius-md` | `8px` | Inputs, selects, buttons, dialogs, alert boxes |
| `--radius-lg` | `12px` | Reserved for larger cards / panels (not yet in use) |

---

## 6. Shadows

| Token | Value | Usage |
|---|---|---|
| `--shadow-sm` | `0 1px 2px 0 rgba(0,0,0,.05)` | Subtle elevation for inputs on focus or hover |
| `--shadow-md` | `0 4px 6px -1px rgba(0,0,0,.07), 0 2px 4px -1px rgba(0,0,0,.04)` | Cards, dropdowns |
| `--shadow-lg` | `0 10px 24px -4px rgba(0,0,0,.10), 0 4px 8px -2px rgba(0,0,0,.05)` | Dialogs/modals |

Dialog box-shadow (component-level): `0 4px 24px rgba(0,0,0,.15)`.

---

## 7. Components

### 7.1 Buttons

#### Primary (submit / confirm action)

| State | Background | Text | Border | Shadow |
|---|---|---|---|---|
| Default | `#0EA5A0` | `#FFFFFF` | none | none |
| Hover | `#0C9490` | `#FFFFFF` | none | `0 4px 12px rgba(14,165,160,.28)` |
| Active/pressed | `#0A8480` | `#FFFFFF` | none | none |
| Focus-visible | `#0EA5A0` | `#FFFFFF` | none | `0 0 0 3px rgba(14,165,160,.30)` |
| Disabled | `#0EA5A0` at 55% opacity | `#FFFFFF` at 55% | none | none |

Sizing: `padding: 0.6–0.75rem 1.25rem`, `border-radius: 8px`, `font-size: 0.9375rem`, `font-weight: 600`.

#### Secondary (cancel / neutral action)

| State | Background | Text | Border | Shadow |
|---|---|---|---|---|
| Default | `#F1F5F9` | `#374151` | `1.5px #E2E8F0` | none |
| Hover | `#E2E8F0` | `#374151` | `1.5px #E2E8F0` | none |
| Disabled | 55% opacity | — | — | none |

#### Danger (destructive / deactivate / reset)

| State | Background | Text | Notes |
|---|---|---|---|
| Default | `#DC2626` | `#FFFFFF` | Used for password reset confirm, deactivation |
| Hover | `#B91C1C` | `#FFFFFF` | |
| Disabled | 55% opacity | | |

#### Table action (inline row button)

| State | Background | Text | Border |
|---|---|---|---|
| Default | `#FFFFFF` | `#374151` | `1.5px #D1D5DB` |
| Hover | `#F9FAFB` | `#374151` | `1.5px #9CA3AF` |
| Danger variant | `#FFFFFF` | `#DC2626` | `1.5px #FECACA` |
| Danger hover | `#FEF2F2` | `#DC2626` | `1.5px #FCA5A5` |

---

### 7.2 Form Inputs

Applies to `<input type="text">`, `<input type="email">`, `<input type="password">`.

| State | Background | Text | Border | Shadow/Ring |
|---|---|---|---|---|
| Default | `#FFFFFF` | `#0F172A` | `1.5px #D1D5DB` | none |
| Placeholder | `#FFFFFF` | `#9CA3AF` | `1.5px #D1D5DB` | none |
| Hover | `#FFFFFF` | `#0F172A` | `1.5px #9CA3AF` | none |
| Focus | `#FFFFFF` | `#0F172A` | `1.5px #0EA5A0` | `0 0 0 3px rgba(14,165,160,.15)` |
| Invalid (touched) | `#FFFFFF` | `#0F172A` | `1.5px #DC2626` | none |
| Invalid + Focus | `#FFFFFF` | `#0F172A` | `1.5px #DC2626` | `0 0 0 3px rgba(220,38,38,.12)` |

Sizing: `padding: 0.65rem 0.875rem`, `border-radius: 8px`, `font-size: 0.9375rem`.

---

### 7.3 Select / Dropdown

Same visual language as inputs. Custom arrow via inline SVG background-image (stroke `#64748B`). Padding-right `2.25rem` to avoid text overlapping arrow.

| State | Border | Ring |
|---|---|---|
| Default | `1.5px #E2E8F0` | none |
| Hover | `1.5px #9CA3AF` | none |
| Focus | `1.5px #0EA5A0` | `0 0 0 3px rgba(14,165,160,.15)` |

---

### 7.4 Badges

All badges: `border-radius: 999px`, `padding: 0.2rem 0.6rem`, `font-size: 0.8rem`, `font-weight: 500`.

| Variant | Background | Text | Usage |
|---|---|---|---|
| Default | `#E2E8F0` | `#2D3748` | Generic / User role |
| Admin | `#EBF8FF` | `#2B6CB0` | Administrator role |
| Active | `#F0FFF4` | `#276749` | Active user status |
| Inactive | `#FEF2F2` | `#C53030` | Inactive user status |

---

### 7.5 Alert / Notification Banners

| Variant | Background | Border | Text |
|---|---|---|---|
| Error | `#FEF2F2` | `1px #FECACA` | `#DC2626` |
| Success | `#F0FDF4` | `1px #86EFAC` | `#166534` |

Sizing: `padding: 0.75rem 1rem`, `border-radius: 8px`, `font-size: 0.875rem`.

---

### 7.6 Tables

| Element | Style |
|---|---|
| Header row background | `#f7fafc` |
| Header border-bottom | `2px solid #E2E8F0` |
| Cell padding | `0.75rem 1rem` |
| Row divider | `1px solid #E2E8F0` |
| Last row | no border-bottom |
| Sort button | no border/bg; `color: #0F172A`; hover `color: #0EA5A0` |

---

### 7.7 Dialogs / Modals

| Property | Value |
|---|---|
| Overlay background | `rgba(0,0,0,.45)` |
| Dialog background | `#FFFFFF` |
| Border radius | `8px` |
| Padding | `2rem` |
| Max width | `95vw` |
| Shadow | `0 4px 24px rgba(0,0,0,.15)` |
| z-index | `1000` |

---

### 7.8 Navigation Sidebar

| Property | Value |
|---|---|
| Width | `220px` |
| Background | `#141E2B` |
| Item padding | `0.6rem 0.875rem` |
| Item border-radius | `8px` |
| Item font-size | `0.875rem`, weight `500` |
| Idle text color | `#8DA0B3` |
| Hover background | `#1D2D40`, text `#CBD5E1` |
| Active background | `#0EA5A0` |
| Active text | `#FFFFFF` |

---

### 7.9 Toolbar

| Property | Value |
|---|---|
| Background | `#FFFFFF` |
| Border | none except `1px #E2E8F0` on bottom |
| Border-radius | `0` |
| Padding | `0.875rem 1.25rem` |
| App title font | `0.9375rem`, weight `700`, letter-spacing `-0.01em` |

---

## 8. Login Screen

Split-panel layout:

| Panel | Width | Background |
|---|---|---|
| Brand (left) | `400px` (flex 0 0 400px) | `#141E2B` with radial teal gradients |
| Form (right) | `flex: 1` | `#F5F4F1` |

Responsive: brand panel hidden at `≤ 680px`; form panel switches to `background: #FFFFFF`.

Brand mark: `font-size: 2.75rem`, weight `800`, color `#0EA5A0`.

---

## 9. Transitions

| Usage | Transition |
|---|---|
| Button background/shadow | `0.15s ease` |
| Input border/shadow | `0.15s ease` |
| Sidebar nav item | `background 0.14s ease, color 0.14s ease` |
| Button press (transform) | `0.1s ease` |

---

## 10. Do / Don't

| ✅ Do | ❌ Don't |
|---|---|
| Reference `--color-*` tokens for all color values in component styles | Hardcode hex values in component styles |
| Use teal (`#0EA5A0`) for all primary interactive/active states | Use the old blue `#4299e1` (legacy, replaced) |
| Set explicit `color` on every button variant | Rely on browser/inheritance for button text color |
| Use `appearance: none` on `<select>` elements | Let the browser render native SELECT chrome |
| Pair `#0F172A` text on `#FFFFFF` / `#F5F4F1` backgrounds | Use `--color-text-muted` (`#94A3B8`) for interactive or body text |
| Use `--radius-md` (8px) for buttons, inputs, dialogs, alerts | Mix 4px and 8px radius without purpose |
