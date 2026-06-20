# mPOS Pro UI direction

## Moodboard

Modern retail software: warm orange CTA, clean white surfaces, high-contrast money totals, generous touch targets, and compact operational information. The interface is deliberately utilitarian rather than social or decorative.

## Token set

| Token | Value | Usage |
|---|---:|---|
| Primary | `#F57C00` | Main POS action, selected state |
| Dark primary | `#E65100` | Brand heading and emphasis |
| Light primary | `#FFF3E0` | Selected filters and non-critical warnings |
| Success | `#2E7D32` | Paid, synced, available |
| Error | `#D32F2F` | Cancel, delete, failure |
| Background | `#FFF8F2` | Screen background |
| Surface | `#FFFFFF` | Cards and panels |
| Text | `#1F2937` | Primary information |

## Components

| Component | XML primitive | Rule |
|---|---|---|
| Primary CTA | `Widget.Mpos.Button.Primary` | Orange, minimum 52dp; use once per screen for the next irreversible action. |
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

Implemented visual baseline: Login, Dashboard, POS phone/tablet, Checkout, Receipt, Shift, Sync Status and Unified Inbox.

Use the same components for the remaining screens: Orders, Products, Customers, Inventory, Reports, Employees and Settings. Their first UI iteration should use list cards, search/filter chips, an empty state, then a detail/form screen; do not duplicate colors or ad-hoc button styles.
