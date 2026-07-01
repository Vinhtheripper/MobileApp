# mPOS Pro UI direction

## Moodboard

Modern retail software: navy top bars, blue CTAs, clean white surfaces, high-contrast money totals, generous touch targets, and compact operational information. The interface is deliberately utilitarian rather than social or decorative.

## Token set

| Token | Value | Usage |
|---|---:|---|
| Primary | `#1677F2` | Main POS action, selected state |
| Dark primary | `#0F5FCA` | Brand heading and emphasis |
| Navy header | `#101B2D` | App bars and high-contrast navigation |
| Light primary | `#EAF3FF` | Selected filters and informational surfaces |
| Success | `#10A760` | Paid, synced, available |
| Error | `#EF4444` | Cancel, delete, failure |
| Background | `#F5F7FB` | Screen background |
| Surface | `#FFFFFF` | Cards and panels |
| Text | `#1F2937` | Primary information |

## Components

| Component | XML primitive | Rule |
|---|---|---|
| Primary CTA | `Widget.Mpos.Button.Primary` | Blue, minimum 52dp; use once per screen for the next irreversible action. |
| Success CTA | `Widget.Mpos.Button.Success` | Confirm completed payment only. |
| Secondary action | `Widget.Mpos.Button.Outline` | Navigation and reversible actions. |
| Card | `bg_surface_card` | White, 16dp radius, subtle border. |
| Status badge | `bg_chip_success`, `bg_chip_orange` | Pair color with text; never rely on color alone. |
| Money total | `TextAppearance.Mpos.Money` | Largest visual weight in checkout/cart. |
| Input/search | `EditText` + `bg_surface_card` | 52dp height and 14–16dp horizontal padding. |

## Responsive rule

- Phone: one-column selling flow, persistent checkout action at the bottom.
- Tablet (`sw600dp`): POS has two panels: product discovery on the left and a 320dp cart/checkout panel on the right.

## Screen rollout

Implemented visual baseline: Login, Dashboard, POS phone/tablet, Checkout, Receipt, Shift, Sync Status, Unified Inbox and management/detail screens.

Use the same components for the remaining screens: Orders, Products, Customers, Inventory, Reports, Employees and Settings. Their first UI iteration should use list cards, search/filter chips, an empty state, then a detail/form screen; do not duplicate colors or ad-hoc button styles.
