---
name: Medical Enterprise System
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f3'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1a1c1c'
  on-surface-variant: '#3e4947'
  inverse-surface: '#2f3131'
  inverse-on-surface: '#f0f1f1'
  outline: '#6e7977'
  outline-variant: '#bdc9c6'
  surface-tint: '#006a63'
  primary: '#005c55'
  on-primary: '#ffffff'
  primary-container: '#0f766e'
  on-primary-container: '#a3faef'
  inverse-primary: '#80d5cb'
  secondary: '#006a61'
  on-secondary: '#ffffff'
  secondary-container: '#86f2e4'
  on-secondary-container: '#006f66'
  tertiary: '#734700'
  on-tertiary: '#ffffff'
  tertiary-container: '#945d00'
  on-tertiary-container: '#ffe6cc'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#9cf2e8'
  primary-fixed-dim: '#80d5cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#00504a'
  secondary-fixed: '#89f5e7'
  secondary-fixed-dim: '#6bd8cb'
  on-secondary-fixed: '#00201d'
  on-secondary-fixed-variant: '#005049'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#f9f9f9'
  on-background: '#1a1c1c'
  surface-variant: '#e2e2e2'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '700'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  numeric-display:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 16px
  card-padding-sm: 16px
  card-padding-lg: 24px
  stack-gap: 12px
---

## Brand & Style
This design system is engineered for a high-stakes enterprise medical telesales environment. The aesthetic merges **Corporate Modern** reliability with **Minimalist** clarity to ensure zero cognitive friction during critical patient interactions. 

The visual narrative focuses on "Medical-Grade Trustworthiness," utilizing a systematic approach to density and hierarchy. It adopts the Material 3 (M3) framework but elevates it with a more disciplined, enterprise-focused structure. The interface should feel sterile but not cold, professional but not intimidating, and highly functional for rapid data entry and retrieval.

## Colors
The palette is rooted in medical authority. The **Primary Teal (#0F766E)** provides a deep, stable anchor for the brand, while the **Secondary Turquoise (#0D9488)** is used for active medical states and key interactions. 

The system defaults to a **Crisp White light mode** to maintain a sanitary, professional feel. In dark mode, it transitions to **Dark Slate (#0F172A)** to reduce eye strain during long shifts while maintaining high contrast. Semantic colors are strictly reserved for status: 
- **Warm Amber** for pending appointments or warnings.
- **Emerald** for successful bookings.
- **Crimson** for critical errors or missed patient follow-ups.

## Typography
We utilize **Inter** across all levels for its exceptional legibility on mobile screens and neutral, systematic character. 

A custom `numeric-display` style is introduced specifically for phone numbers and appointment durations, ensuring these critical data points are immediately scannable. All headlines use a semi-bold or bold weight to provide a clear content hierarchy. Line heights are slightly increased from standard defaults to improve readability in dense medical record views.

## Layout & Spacing
The layout follows a **Fluid Grid** model optimized for high-density information. On mobile, we use a 4-column grid with 16dp margins; on tablets, this expands to an 8-column grid with 24dp margins.

Spacing follows an 8dp baseline grid. **Generous internal card padding** (16dp for standard, 24dp for patient profiles) is mandatory to prevent the UI from feeling claustrophobic. Vertical stack gaps should remain consistent at 12dp to maintain a tight relationship between related medical entries while providing enough "breathing room" for touch targets.

## Elevation & Depth
In alignment with Material 3, we use **Tonal Layers** rather than heavy shadows to define depth. The primary background is the lowest level (Level 0). Cards and surface containers sit at Level 1, utilizing a subtle tonal tint of the primary color to distinguish themselves from the background.

Shadows are used sparingly, reserved only for floating action buttons (FABs) and elevated dialogs. When used, shadows should be highly diffused (16% opacity) with a slight teal tint to maintain color harmony. This "flat-plus" approach ensures the UI feels modern and performs well on varied Android hardware.

## Shapes
We adopt the **Rounded (0.5rem)** standard for the majority of UI components. This provides a balance between the "friendly" nature of medical care and the "structured" nature of enterprise software. 

- **Small Components (Buttons, Inputs):** 8px corner radius.
- **Medium Components (Cards, Dialogs):** 16px corner radius.
- **Large Components (Bottom Sheets):** 24px top-corner radius.
- **Search Bars:** Pill-shaped (Level 3) to differentiate navigation and utility from data-entry elements.

## Components

### Buttons
- **Filled:** Use Primary Teal for the main call-to-action (e.g., "Start Call").
- **Tonal:** Use for secondary actions within a section (e.g., "Add Note").
- **Outlined:** Use for neutral actions or "Cancel" states to minimize visual weight.

### Cards & Lists
Patient records must be housed in Level 1 cards with 16dp padding. Use 1px subtle borders in dark mode to define boundaries. Lists within cards should use 56dp minimum height for touch targets.

### Input Fields
Filled text fields with a bottom-line stroke are preferred for rapid data entry. The active state must use the Primary Teal with a 2px stroke width. Labels must always be visible (floating) when the field is populated.

### Status Indicators
Use high-contrast chips for patient status (e.g., "Confirmed," "No Show"). These should utilize the accent colors (Emerald, Crimson, Amber) with a 10% opacity background of the same hue to ensure the text remains the focal point.

### Checkboxes & Switches
Switches use the Secondary Turquoise for the "On" state. Checkboxes should be large (24x24dp touch target) with a distinct checkmark animation to provide tactile feedback to the user during multi-select medical checklists.

### Icons
Use **Material Symbols (Rounded)**. Icons related to medical actions (stethoscopes, calendars, tooth icons for dental context) should be consistent in stroke weight (2px) to match the Inter typeface's visual density.