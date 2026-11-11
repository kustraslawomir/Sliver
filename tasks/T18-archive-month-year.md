# T18 — Archive: month + year

Milestone: M3 Archive  
Depends on: T05, T10

## Goal
The calendar.

## Do
- Header 'N SECONDS · SINCE <firstDay>' + settings gear.
- MONTH grid (Mon-first, prev/next month bounded by first day…today) with cell states from DESIGN; empty past → Add sheet (Video from gallery / Photo → 1 s with forDate, Leave it open); today → Camera; filled → Day detail.
- YEAR view: 12 rows × 31 cells colored by thumbnail average color (compute once on save, store in Day as `color`); tap month → grid. Links 'Fill the past from your gallery' and 'Make your <year> film so far'.
- Memory card: pick the most recent of 1 year / 100 days / 1 month ago that has a second.

## Don't
- No streak anything.

## Done when
- [ ] Grid renders 6 months of data smoothly
- [ ] Add flow saves to the chosen date
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
