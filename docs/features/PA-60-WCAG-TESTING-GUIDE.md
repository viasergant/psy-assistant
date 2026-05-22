# PA-60 Phase 3: WCAG Viewport Testing Guide

## Overview
This document provides testing instructions for validating that Ukrainian translations (which are 40-260% longer than English) do not cause layout issues across different viewport sizes.

**Acceptance Criteria:** Zero text overflow/truncation on 320px, 768px, 1024px viewports across 5 sampled pages.

---

## Test Configuration

### Viewports to Test
1. **Mobile (320px width)** - Minimum WCAG AA requirement
2. **Tablet (768px width)** - Common tablet portrait
3. **Desktop (1024px width)** - Small desktop/laptop

### Languages to Test
- ✅ English (baseline)
- ✅ Ukrainian (longer text - primary test focus)

### Browser DevTools Setup
**Chrome/Edge:**
1. Open DevTools (F12)
2. Click "Toggle device toolbar" (Ctrl+Shift+M / Cmd+Shift+M)
3. Select "Responsive" mode
4. Set width to 320px, 768px, or 1024px
5. Test in both landscape and portrait orientations

**Firefox:**
1. Open DevTools (F12)
2. Click "Responsive Design Mode" (Ctrl+Shift+M / Cmd+Shift+M)
3. Enter custom dimensions

---

## Pages to Test

Based on converted components with the most translation keys:

### 1. **Therapist Profile Wizard** (`/therapists/profile-wizard`)
**Path:** `frontend/src/app/features/therapists/profile-wizard/`  
**Why:** 4-step wizard with ~50 translation keys, complex form layouts, character counters  
**Critical Areas:**
- Step headers and navigation
- Form field labels (esp. long Ukrainian labels like "Електронна пошта")
- Character counters with interpolation
- Action buttons in footer
- Validation messages

**Test Checklist:**
- [ ] 320px EN: All 4 steps render without horizontal scroll
- [ ] 320px UK: All labels visible, no text truncation
- [ ] 768px EN: Form layout comfortable, 2-column grid works
- [ ] 768px UK: Labels don't wrap awkwardly
- [ ] 1024px EN: Optimal spacing, professional appearance
- [ ] 1024px UK: No excessive whitespace from longer text

---

### 2. **Client Detail Profile** (`/clients/:id`)
**Path:** `frontend/src/app/features/clients/client-detail/`  
**Why:** 9 sections with 47 field labels, emergency contact, communication preferences  
**Critical Areas:**
- Section headers (e.g., "Контакт для екстрених випадків" - 30 chars)
- Two-column grid layout with field labels
- Tag input placeholder
- Badge ("Клієнт" vs "Client")
- Long field labels like "Бажаний метод" for "Preferred method"

**Test Checklist:**
- [ ] 320px EN: Single-column layout for all sections
- [ ] 320px UK: Field labels stack properly, no overlap
- [ ] 768px EN: Two-column grid visible
- [ ] 768px UK: Grid doesn't break with longer labels
- [ ] 1024px EN: Comfortable spacing between fields
- [ ] 1024px UK: Labels aligned, no grid collapse

---

### 3. **Lead List with Dialogs** (`/leads`)
**Path:** `frontend/src/app/features/leads/components/`  
**Why:** Table headers, filters, create/edit/convert dialogs, pagination  
**Critical Areas:**
- Page title "Leads" → "Потенційні клієнти" (+260%)
- Table headers in narrow columns
- Filter dropdowns with long status labels
- Dialog titles (e.g., "Створити потенційного клієнта" - 29 chars)
- Pagination controls

**Test Checklist:**
- [ ] 320px EN: Table scrolls horizontally if needed
- [ ] 320px UK: Headers don't break table structure
- [ ] 768px EN: All columns visible without scroll
- [ ] 768px UK: Table remains usable, columns resize
- [ ] 1024px EN: Professional table appearance
- [ ] 1024px UK: No column squashing from headers

---

### 4. **Admin Therapist List** (`/admin/therapists`)
**Path:** `frontend/src/app/features/admin/therapists/components/therapist-list/`  
**Why:** Multiple filters, table with specializations column, pagination  
**Critical Areas:**
- Filter labels (Role, Status)
- Table headers ("Спеціалізації" - 13 chars)
- Status badges (Active/Inactive)
- Action buttons
- Pagination aria-labels

**Test Checklist:**
- [ ] 320px EN: Filters stack vertically
- [ ] 320px UK: Filter labels visible, dropdowns work
- [ ] 768px EN: Table fits, all columns readable
- [ ] 768px UK: Longer specialization names don't break layout
- [ ] 1024px EN: Professional admin interface
- [ ] 1024px UK: Adequate column widths

---

### 5. **Schedule Calendar** (`/schedule`)
**Path:** `frontend/src/app/features/schedule/components/schedule-calendar/`  
**Why:** Calendar grid with cells, legend, "Today" button, status labels  
**Critical Areas:**
- "Today" button: "Today" → "Сьогодні" (+60%)
- Cell status labels (Pending, Leave, Override, Booked)
- Legend items with color keys
- Calendar header with week navigation
- Time slot labels

**Test Checklist:**
- [ ] 320px EN: Calendar switches to mobile view
- [ ] 320px UK: Button labels fit, no truncation
- [ ] 768px EN: Weekly view clear and usable
- [ ] 768px UK: Status labels in cells remain visible
- [ ] 1024px EN: Full calendar with comfortable spacing
- [ ] 1024px UK: Legend items don't wrap awkwardly

---

## Testing Procedure

### Step-by-Step for Each Page

1. **Load page in English**
   - Set viewport width (320px/768px/1024px)
   - Take screenshot for baseline
   - Note any layout issues (should be none)

2. **Switch to Ukrainian**
   - Use language switcher in top-right
   - Verify instant switch (<300ms per AC)
   - Compare against English baseline

3. **Check for Issues**
   - ❌ **Text Overflow:** Text extends beyond container
   - ❌ **Truncation:** Text cut off with ellipsis or hidden
   - ❌ **Horizontal Scroll:** Page requires horizontal scrolling
   - ❌ **Overlap:** Text overlaps other elements
   - ❌ **Breaking Layout:** Grid collapses, buttons stack badly
   - ✅ **Acceptable:** Text wraps to multiple lines within container

4. **Document Findings**
   - Use table below to track results
   - For failures, note:
     - Viewport size
     - Component name
     - Issue type (overflow/truncation/scroll/etc.)
     - Screenshot filename

---

## Test Results Matrix

| Page | Viewport | Language | Status | Issues | Screenshot |
|------|----------|----------|--------|--------|------------|
| Therapist Wizard - Step 1 | 320px | EN | ⬜ | | |
| Therapist Wizard - Step 1 | 320px | UK | ⬜ | | |
| Therapist Wizard - Step 2 | 320px | EN | ⬜ | | |
| Therapist Wizard - Step 2 | 320px | UK | ⬜ | | |
| Therapist Wizard - Step 3 | 320px | EN | ⬜ | | |
| Therapist Wizard - Step 3 | 320px | UK | ⬜ | | |
| Therapist Wizard - Step 4 | 320px | EN | ⬜ | | |
| Therapist Wizard - Step 4 | 320px | UK | ⬜ | | |
| Client Detail | 320px | EN | ⬜ | | |
| Client Detail | 320px | UK | ⬜ | | |
| Lead List | 320px | EN | ⬜ | | |
| Lead List | 320px | UK | ⬜ | | |
| Admin Therapist List | 320px | EN | ⬜ | | |
| Admin Therapist List | 320px | UK | ⬜ | | |
| Schedule Calendar | 320px | EN | ⬜ | | |
| Schedule Calendar | 320px | UK | ⬜ | | |
| | | | | | |
| Therapist Wizard - Step 1 | 768px | EN | ⬜ | | |
| Therapist Wizard - Step 1 | 768px | UK | ⬜ | | |
| Therapist Wizard - Step 2 | 768px | EN | ⬜ | | |
| Therapist Wizard - Step 2 | 768px | UK | ⬜ | | |
| Therapist Wizard - Step 3 | 768px | EN | ⬜ | | |
| Therapist Wizard - Step 3 | 768px | UK | ⬜ | | |
| Therapist Wizard - Step 4 | 768px | EN | ⬜ | | |
| Therapist Wizard - Step 4 | 768px | UK | ⬜ | | |
| Client Detail | 768px | EN | ⬜ | | |
| Client Detail | 768px | UK | ⬜ | | |
| Lead List | 768px | EN | ⬜ | | |
| Lead List | 768px | UK | ⬜ | | |
| Admin Therapist List | 768px | EN | ⬜ | | |
| Admin Therapist List | 768px | UK | ⬜ | | |
| Schedule Calendar | 768px | EN | ⬜ | | |
| Schedule Calendar | 768px | UK | ⬜ | | |
| | | | | | |
| Therapist Wizard - Step 1 | 1024px | EN | ⬜ | | |
| Therapist Wizard - Step 1 | 1024px | UK | ⬜ | | |
| Therapist Wizard - Step 2 | 1024px | EN | ⬜ | | |
| Therapist Wizard - Step 2 | 1024px | UK | ⬜ | | |
| Therapist Wizard - Step 3 | 1024px | EN | ⬜ | | |
| Therapist Wizard - Step 3 | 1024px | UK | ⬜ | | |
| Therapist Wizard - Step 4 | 1024px | EN | ⬜ | | |
| Therapist Wizard - Step 4 | 1024px | UK | ⬜ | | |
| Client Detail | 1024px | EN | ⬜ | | |
| Client Detail | 1024px | UK | ⬜ | | |
| Lead List | 1024px | EN | ⬜ | | |
| Lead List | 1024px | UK | ⬜ | | |
| Admin Therapist List | 1024px | EN | ⬜ | | |
| Admin Therapist List | 1024px | UK | ⬜ | | |
| Schedule Calendar | 1024px | EN | ⬜ | | |
| Schedule Calendar | 1024px | UK | ⬜ | | |

**Legend:**
- ⬜ Not tested
- ✅ Pass (no issues)
- ⚠️ Minor issue (cosmetic, doesn't block)
- ❌ Fail (blocks acceptance)

---

## Known Length Variance (For Context)

### Critical Translations (>60% longer)
These are accurate translations that **require** layout flexibility:

| Key | English | Ukrainian | Variance |
|-----|---------|-----------|----------|
| `nav.leads` | "Leads" (5) | "Потенційні клієнти" (18) | +260% |
| `nav.admin` | "Admin" (5) | "Адміністрування" (15) | +200% |
| `leads.list.filterLabel` | "Filter leads" (12) | "Фільтрувати потенційних клієнтів" (32) | +166% |
| `clients.detail.fields.email` | "Email" (5) | "Електронна пошта" (16) | +220% |
| `clients.detail.fields.allowPhone` | "Allow phone" (11) | "Дозволити телефонні дзвінки" (27) | +145% |
| `schedule.calendar.today` | "Today" (5) | "Сьогодні" (8) | +60% |

**These are NOT translation errors** - they reflect natural Ukrainian language structure.

---

## Common WCAG Failures and Fixes

### Issue: Text Overflows Container
**Symptoms:** Text extends beyond button/card/cell boundaries  
**Fix:** Add `overflow-wrap: break-word` or `text-overflow: ellipsis`

### Issue: Horizontal Scroll on Mobile
**Symptoms:** Page wider than 320px viewport  
**Fix:** Use `max-width: 100%` on containers, responsive grid

### Issue: Button Text Truncated
**Symptoms:** "..." appears on important action buttons  
**Fix:** Allow buttons to expand with `width: auto`, increase padding

### Issue: Table Columns Collapse
**Symptoms:** Table headers or data unreadable  
**Fix:** Use horizontal scroll for table, or card layout on mobile

### Issue: Grid Breaks on Tablet
**Symptoms:** Two-column layout doesn't work with longer labels  
**Fix:** Use `grid-template-columns: minmax(200px, 1fr) 1fr` for flexibility

---

## Automated Testing (Future Enhancement)

While Phase 3 requires manual testing, consider adding automated visual regression tests:

```typescript
// Example Playwright test for viewport testing
test('therapist wizard handles Ukrainian at 320px', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 568 });
  await page.goto('/therapists/profile-wizard');
  
  // Switch to Ukrainian
  await page.click('[data-testid="language-switcher"]');
  await page.click('[data-lang="uk"]');
  
  // Check for horizontal scroll
  const hasHorizontalScroll = await page.evaluate(() => 
    document.body.scrollWidth > document.body.clientWidth
  );
  expect(hasHorizontalScroll).toBe(false);
  
  // Visual regression
  await expect(page).toHaveScreenshot('wizard-step1-uk-320px.png');
});
```

---

## Sign-Off

**Tested By:** _________________  
**Date:** _________________  
**Pass Criteria:** All 5 pages × 3 viewports × 2 languages = 30 test cases must pass  
**Actual Pass Rate:** _____ / 30  

**Notes:**

---

**Related Documents:**
- [docs/i18n-key-structure.md](docs/i18n-key-structure.md) - Translation key naming
- [PA-60-IMPLEMENTATION-SUMMARY.md](PA-60-IMPLEMENTATION-SUMMARY.md) - Full implementation details
- [frontend/scripts/validate-i18n-*.js](frontend/scripts/) - CI validation scripts
