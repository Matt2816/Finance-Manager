# Cursor Design Guidelines

## Core Principle

**Design mobile first.**

Every interface MUST be designed, implemented, and tested on the smallest supported viewport before expanding to tablet and desktop layouts.

Desktop is an enhancement, not the starting point.

## Priority Order

Apply these priorities in order when trade-offs are required:

1. Mobile usability
2. Accessibility
3. Simplicity
4. Performance
5. Responsiveness
6. Desktop enhancements

Every design decision should reduce cognitive load while making the interface intuitive and efficient.

## Supported Viewports

All pages MUST work at the following widths (minimum):

- Small phones: `320px`
- Standard phones: `375px` to `430px`
- Tablets: `768px`
- Small laptops: `1024px`
- Desktop: `1280px+`
- Large monitors: `1440px+`

Layouts MUST adapt without horizontal page scrolling.

## Breakpoint Strategy

Use content-driven breakpoints first; use these defaults unless a component needs custom behavior:

- `sm`: `>= 320px`
- `md`: `>= 768px`
- `lg`: `>= 1024px`
- `xl`: `>= 1280px`
- `2xl`: `>= 1440px`

Rules:

- Start from the mobile base style, then layer larger breakpoints.
- Avoid device-specific targeting.
- Avoid creating unique layouts per device model.

## Mobile-First Workflow

For every new page or component:

1. **Mobile (320px to 430px)**
   - Use a single-column layout by default.
   - Ensure touch targets are comfortable and reachable.
   - Surface primary actions early.
   - Keep hierarchy clear and scanning-friendly.
2. **Tablet (`>=768px`)**
   - Increase spacing and breathing room.
   - Introduce multi-column layouts where content benefits.
   - Preserve the same information architecture.
3. **Desktop (`>=1024px`)**
   - Expand density carefully (sidebars, secondary panels, wider grids).
   - Add contextual information without displacing primary actions.
   - Ensure full keyboard usability and visible focus flow.

Never design desktop first and scale down.

## Layout Principles

Prefer:

- CSS Grid for page-level and dense data layouts
- Flexbox for one-dimensional alignment
- Fluid spacing and responsive containers
- Relative units (`rem`, `%`, `clamp()`, `min()`, `max()`)

Avoid:

- Fixed pixel widths for core layout containers
- Fixed viewport heights unless required by the use case
- Horizontal overflow as a layout mechanism
- Absolute positioning for primary layout structure
- Overflow clipping that hides essential content

## Spacing and Sizing Tokens

Use a consistent spacing scale:

- `4, 8, 12, 16, 24, 32, 48, 64`

Guidance:

- Use smaller steps (`4-16`) within components.
- Use larger steps (`24-64`) between sections.
- Keep rhythm consistent across pages.

## Typography

Prioritize readability and hierarchy.

Requirements:

- Minimum body text size: `16px` equivalent
- Line height: `1.4` to `1.7`
- Clear heading hierarchy (H1 to H6 used semantically)
- Reasonable desktop line length (target `45-80` characters)
- Never rely on color alone to communicate meaning

## Interaction and Touch Targets

Interactive elements MUST be touch-friendly.

Requirements:

- Minimum target size: `44x44px`
- Adequate spacing between adjacent controls
- Visible and consistent interactive affordances
- Sticky actions only when they improve completion
- Icons without labels only when meaning is universally clear

## Navigation

Mobile navigation should be:

- Simple
- Predictable
- Reachable with one hand

Desktop navigation may expand, but MUST preserve the same information architecture and naming.

Do not hide critical actions behind deep interaction chains.

## Component Standards

Reusable components MUST be:

- Responsive
- Accessible
- Keyboard navigable
- Theme-aware (including dark mode where supported)
- Composable and reusable
- Self-contained in behavior and styling boundaries

Components should adapt gracefully to available space without breaking content.

## Forms

Optimize forms for completion speed and error recovery.

Requirements:

- Single-column on mobile
- Visible labels (not placeholder-only)
- Input mode and keyboard hints where applicable
- Inline validation with clear timing (on blur or submit)
- Helpful, specific error messages
- Logical grouping and progressive disclosure for complexity
- Strong, visible focus indicators
- Preserve user-entered values on recoverable failures

## Buttons and Action Hierarchy

Use clear button hierarchy:

- Primary
- Secondary
- Tertiary
- Destructive

Every button state MUST be defined:

- Default
- Hover (pointer contexts)
- Active/pressed
- Focus-visible
- Disabled
- Loading

## Data Display: Cards and Tables

Cards should:

- Use consistent padding and spacing
- Scale naturally across breakpoints
- Avoid unnecessary nesting
- Preserve clear content grouping

Tables on mobile:

- Prefer row-to-card transformations for dense data
- Allow horizontal scrolling only when unavoidable
- Pin or prioritize critical columns

## Responsive Media

Images and media should:

- Scale fluidly within containers
- Preserve intended aspect ratios
- Use responsive sizing (`srcset`, `sizes`) when available
- Avoid cumulative layout shift via explicit dimensions/aspect ratio
- Lazy-load non-critical assets

## Accessibility Requirements

Accessibility is mandatory, not optional.

Requirements:

- Semantic HTML first
- Logical tab order and full keyboard operation
- Proper heading hierarchy
- ARIA only where native semantics are insufficient
- Screen reader-friendly labels and announcements
- WCAG AA contrast minimums
- Persistent, visible focus indicators
- Meaning not conveyed by color alone

## Color and Theming

Use color intentionally and consistently.

Requirements:

- Accessible contrast in all states
- Semantic status colors (success, warning, error, info)
- Consistent palette usage across components
- Support for light/dark themes where product scope includes both

## Motion

Motion should support comprehension, not distract.

Requirements:

- Short, purposeful transitions
- No animation that blocks user interaction
- Respect `prefers-reduced-motion`
- Avoid excessive or looping decorative motion

## Performance

Performance is UX.

Guidelines:

- Minimize JavaScript shipped to the client
- Lazy-load heavy modules/assets
- Optimize media and font loading
- Avoid unnecessary re-renders
- Prefer CSS-driven effects over JavaScript animation when possible

## Content Hierarchy

Users should quickly understand:

1. Where they are
2. What they can do
3. What matters most
4. What to do next

Establish hierarchy with layout, spacing, typography, and grouping before adding decorative styling.

## Empty, Loading, and Error States

Every screen and major component should define all three states.

- **Empty**
  - Explain why content is empty.
  - Offer a clear next action.
  - Avoid dead ends.
- **Loading**
  - Avoid blank screens.
  - Use skeletons/progress indicators where they improve perception.
  - Use optimistic updates when safe and reversible.
- **Error**
  - Explain what happened in plain language.
  - Provide recovery paths.
  - Preserve user input whenever possible.

## QA and Definition of Done

A UI change is not complete until all checks pass:

- Works at `320px` width without horizontal page scroll
- All primary flows are touch-friendly and keyboard accessible
- Readability and hierarchy remain clear across breakpoints
- Forms are completable with clear validation and recovery paths
- Images/media scale without layout shift
- Navigation remains predictable at all viewport sizes
- Focus states are visible and consistent
- Screen reader labels and semantics are present
- Performance remains smooth on mid-range mobile devices

## AI Agent Implementation Rules

When generating UI code:

1. Build the complete mobile experience first.
2. Add tablet and desktop enhancements without changing core information architecture.
3. Use responsive CSS patterns, not device hacks.
4. Implement reusable, composable components.
5. Follow accessibility and keyboard interaction standards by default.
6. Favor clarity and speed over visual complexity.
7. Ensure interactions work for touch, mouse, and keyboard.
8. Deliver production-ready responsive code consistent with the design system.

