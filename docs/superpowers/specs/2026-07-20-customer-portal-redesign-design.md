# Customer portal visual redesign (Revolut-inspired)

## Context

The README frames this project's goal as demonstrating "how software, good UX, and thoughtful
system design can dramatically improve the customer experience," citing biometric/OTP-driven
banking apps as the bar. The customer portal is called out as "the primary product." The current
UI doesn't clear that bar visually:

- `globals.css` is still the unmodified Next.js starter file — no brand palette, no type scale,
  no radius/elevation tokens. `--font-sans` is declared but `body` hardcodes
  `Arial, Helvetica, sans-serif`, so the loaded Geist font isn't even applied.
- All components (`button`, `card`, `input`, etc.) are stock shadcn/ui defaults on the default
  Tailwind `slate`/`blue` palette — functional (variants, focus states are fine) but visually
  indistinguishable from a generic admin template. No brand identity anywhere.

Scope: **customer-portal only** (this app). Employee portal is a separate, lower-visual-stakes
internal console and is out of scope for this pass — this project explicitly improves
"gradually," this is the first pass, not the last.

## Design system

**Color** (light base + dark-mode tokens from the start, not the leftover starter media query):

| Token | Light | Dark | Use |
|---|---|---|---|
| `--ink` | `#151222` | `#F2F0FA` | primary text |
| `--surface` | `#FAF9FE` | `#0F0B22` | page background |
| `--card` | `#FFFFFF` | `#1B1533` | card surface |
| `--accent` | `#5B3DF5` | `#8B7CFF` | primary actions, brand |
| `--accent-deep` | `#241653` | `#150C33` | hero gradient dark end |
| `--success` | `#12B76A` | `#3DD68C` | approved |
| `--danger` | `#E5484D` | `#FF6369` | rejected/errors |
| `--warning` | `#F5A524` | `#FFC24D` | pending/manual review |

Chosen deliberately away from Tailwind's stock `indigo-600`/`slate` so the palette reads as a
brand decision, not an unstyled default. Success/danger/warning stay visually distinct from the
accent so status is never ambiguous against a purple-heavy UI.

**Type** — no new font added. Geist Sans (already loaded, currently unused on `body` due to the
hardcoded `Arial` override) becomes the single UI/heading family. Geist Mono (already loaded,
currently entirely unused) becomes the signature move: every currency figure, account number, and
OTP digit renders in Geist Mono with `font-variant-numeric: tabular-nums`. This is the
fintech-standard treatment (Stripe, Revolut, Monzo) for making monetary figures read as precise
rather than typed inline with prose — grounded in this being a money product, not a decorative
pairing.

**Layout & signature element** — mobile-first single column shape is kept (already correct for
this product). The limit summary card (`LimitSummaryCard`) becomes a dark indigo→
`--accent-deep` gradient "hero" card: daily limit / used / remaining as large tabular-mono
figures over a subtle radial glow, thin progress bar for usage. A smaller variant of the same
treatment reappears on the request list/detail views so it reads as one system. Everything else
stays flat and quiet — `rounded-2xl`, no shadows outside the hero card, generous padding — so the
hero card is the one place spending visual boldness (per the "spend your boldness in one place"
principle).

**Motion** — restrained, respects `prefers-reduced-motion`: button press scale, OTP digit-box
focus ring, wizard step-progress bar fill on transition. Existing `Skeleton` shimmer kept as-is.
No page-transition choreography.

## Implementation surface

1. **`globals.css`** — full token rewrite per the table above, both `:root` and a dark-mode
   block. Fix `body` to actually use `var(--font-sans)` instead of the hardcoded `Arial`
   fallback.
2. **Base components** (`src/components/ui/*`) — re-themed once against the new CSS variables
   (via Tailwind's `@theme inline` mapping, same mechanism already in place) so every page
   inherits the new look without per-page overrides. Existing variant/size structure (`cva`)
   is kept — this is a re-skin, not a rewrite of component APIs.
3. **Bespoke treatment** (beyond the mechanical token swap):
   - **Login** — brand mark + accent gradient touch on the existing centered-card layout.
   - **Dashboard** — `LimitSummaryCard` becomes the signature gradient hero card.
   - **Increase-limit wizard** (`increase-limit/page.tsx` and its steps) — the README's actual
     centerpiece (choose limit → reason → review → OTP → biometric). Step indicator, OTP digit
     input in tabular mono, biometric step visual treatment.
   - **Request detail timeline** (`components/requests/timeline.tsx`) — status color mapping
     onto the new success/danger/warning tokens.
4. **Mechanical reskin, no bespoke redesign needed**: notifications page, requests list, profile
   page, `topbar`/`bottom-nav` — these inherit correctly once the base components and tokens are
   updated.

## Testing / verification

This is a visual change — type-checking and existing component tests don't verify it looks
right. Per project convention: run the dev server and actually view each redesigned screen in a
browser (light + dark, mobile viewport width, since this is a mobile-first single-column app)
before calling this done. No new automated visual-regression tooling is being introduced for
this pass — out of scope, would be a separate decision.

## Out of scope (this pass)

- Employee portal (separate app, separate visual-stakes decision).
- Any new font/dependency — the redesign works entirely within already-loaded fonts and the
  already-installed Tailwind/shadcn stack.
- Automated visual regression testing.
- Backend/API changes — this is a pure frontend re-skin, no contract changes.
