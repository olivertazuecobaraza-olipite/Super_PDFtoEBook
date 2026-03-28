# Design System Specification: The Digital Alchemist

## 1. Overview & Creative North Star
The North Star for this design system is **"The Digital Alchemist."** 

Standard document converters feel like utilitarian office equipment—clunky, gray, and mechanical. This system reimagines the conversion process as a premium, "editorial" transformation. We move away from the "tool" aesthetic and toward a "studio" experience. 

By leveraging **Intentional Asymmetry** and **Tonal Depth**, we break the rigid, boxed-in grid of traditional software. We use high-contrast typography scales (the bold authority of Manrope paired with the technical precision of Inter) to create a layout that feels like a high-end digital magazine. The interface doesn't just "process" files; it elevates them.

---

## 2. Colors & Surface Philosophy
The palette is rooted in a deep, nocturnal foundation, punctuated by the high-energy "Electric Indigo" to signal action and progress.

### The "No-Line" Rule
**Borders are a failure of hierarchy.** In this system, 1px solid borders are strictly prohibited for sectioning. We define boundaries through:
1.  **Background Color Shifts:** A `surface-container-low` section sitting on a `surface` background.
2.  **Tonal Transitions:** Using the depth of `secondary-container` to anchor a functional area.

### Surface Hierarchy & Nesting
Think of the UI as stacked sheets of fine, semi-translucent paper.
*   **Base:** `surface` (#f8f9ff)
*   **Layer 1 (The Desk):** `surface-container-low` (#eff4ff) for large grouping areas.
*   **Layer 2 (The Document):** `surface-container-lowest` (#ffffff) for primary interactive cards.
*   **Layer 3 (The Focus):** `surface-container-highest` (#d5e3fd) for active states or contextual menus.

### The "Glass & Gradient" Rule
To escape the "flat" look, use Glassmorphism for floating elements (like the Conversion History). 
*   **Formula:** `surface` color at 60% opacity + `backdrop-blur: 20px`.
*   **Signature Textures:** Apply a linear gradient from `primary` (#000666) to `primary-container` (#1a237e) on main CTAs to provide a sense of "visual soul" and depth.

---

## 3. Typography: Editorial Precision
We use a dual-font strategy to balance character with readability.

*   **Display & Headlines (Manrope):** These are our "Editorial" voices. Use `display-lg` and `headline-md` with tight letter-spacing (-0.02em) to create a sense of premium authority.
*   **Body & UI (Inter):** These are our "Technical" voices. Inter provides the legibility required for file names, metadata, and status labels.

**Key Scale Reference:**
*   **Hero Headers:** `display-sm` (Manrope, 2.25rem) — Used for welcome states.
*   **Section Titles:** `title-lg` (Inter, 1.375rem, Medium weight) — Used for the "Conversion Queue."
*   **Data Points:** `label-md` (Inter, 0.75rem, All Caps) — Used for file extensions (PDF, EPUB).

---

## 4. Elevation & Depth
We achieve hierarchy through **Tonal Layering** rather than structural lines.

### The Layering Principle
Avoid shadows on static elements. Instead, place a `surface-container-lowest` card on a `surface-container-low` background. This creates a "soft lift" that feels architectural rather than "pasted on."

### Ambient Shadows
When an element must float (e.g., a modal or a primary upload button), use an **Ambient Shadow**:
*   **Values:** `0px 24px 48px rgba(13, 28, 47, 0.06)`
*   **Color:** Always tint your shadow with the `on-surface` color (#0d1c2f) at very low opacity. Never use pure black.

### The "Ghost Border" Fallback
If contrast is legally required for accessibility, use the **Ghost Border**:
*   `outline-variant` (#c6c5d4) at **15% opacity**. It should be felt, not seen.

---

## 5. Components & Layout Patterns

### File Upload Zone (The Alchemist’s Crucible)
*   **Container:** Large `xl` (1.5rem) rounded corners. 
*   **State:** Use a subtle `tertiary-container` pulse animation when a file is hovered over the zone.
*   **Feedback:** Transition the background from `surface-container` to a soft `primary-fixed` tint upon successful drop.

### Conversion History (The Glass List)
*   **Style:** No dividers. Use `md` (0.75rem) spacing between items.
*   **Surface:** Apply the Glassmorphism formula. Each item should have a 1px "Ghost Border" at 10% opacity to catch the light.
*   **Separation:** Use vertical white space (`spacing-6`) instead of horizontal lines.

### Buttons (Primary Action)
*   **Style:** Gradient fill (`primary` to `primary-container`). 
*   **Shape:** `full` (pill-shaped) for the main conversion trigger; `lg` (1rem) for secondary actions.
*   **Interaction:** On hover, the shadow spread increases by 8px, and the gradient slightly brightens.

### Progress Indicators
*   **Track:** `surface-variant`.
*   **Indicator:** `tertiary` (Electric Indigo) with a subtle outer glow (bloom) using the same color at 30% opacity.

---

## 6. Do's and Don'ts

### Do
*   **Do** use asymmetrical layouts. Place the "Upload" card slightly off-center or overlapping a large `display-lg` background text for a bespoke feel.
*   **Do** use the `spacing-20` scale for "hero" margins to allow the design to breathe.
*   **Do** use `on-surface-variant` for secondary metadata to create a clear visual hierarchy.

### Don't
*   **Don't** use 100% opaque borders. It breaks the "Digital Alchemist" immersion.
*   **Don't** use standard "Drop Shadows" from software defaults. 
*   **Don't** use more than one "Electric Indigo" (`tertiary`) element per screen. It is a high-energy accent and should be used only for the most important focal point.
*   **Don't** use dividers in lists. If the content feels cluttered, increase the `spacing` token rather than adding a line.