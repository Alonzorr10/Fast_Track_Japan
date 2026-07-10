# Tokyo ward garbage schedule — data sourcing status

This document exists because the garbage-schedule feature has an honest data problem: the app is built for **all 23 special wards of Tokyo**, but only one ward (Setagaya) currently has real, accurate collection-day data seeded. Ward *names* are seeded so the dropdown populates, but selecting any other ward reveals no districts and no schedule. This is a data-sourcing problem, not a code problem.

## What is seeded

Run `docs/data/2026-07-10-tokyo-23-wards-seed.sql` in the Supabase SQL Editor to insert all 23 ward names into `garbage_wards`. The migration is idempotent — it skips wards that are already present, so it is safe to run alongside or after the base schema in `docs/superpowers/db/2026-07-03-garbage-schema.sql`.

After running the ward seed, the app's UI state per ward is:

| Ward | Names | Chōme (districts) | Schedule | Notification worker |
|---|---|---|---|---|
| Setagaya (三軒茶屋一丁目) | ✅ | ✅ 1 example area | ✅ | ✅ fires correctly |
| The other 22 wards | ✅ (name only) | ❌ empty | ❌ | ❌ nothing to remind |

A user in the other 22 wards can see their ward in the dropdown but cannot complete garbage setup. That is honest — it is better than silently pretending we have data.

## What is required to seed a new ward

For each ward we want to actually support end-to-end, three things need to happen:

1. **Chōme dataset** — at least one row per chōme (district) in that ward, inserted into `garbage_areas` with `wardCode` matching the slug from the ward seed. Real ward geographies list hundreds of chōme; seeding a subset is fine as long as users can pick something reasonable.
2. **Weekly recurrence** — for each `garbage_categories.code` that ward observes (BURNABLE, NON_BURNABLE, RECYCLABLE, PLASTIC, PAPER, CANS_BOTTLES, PET_BOTTLE, and the info-only OVERSIZED), insert one or more rows in `garbage_schedules` with `weekday` (1–7 ISO) and, when the collection is on the Nth weekday of the month rather than every week, `weeksOfMonth` as an int array (e.g. `{2,4}` for 2nd and 4th).
3. **Verification** — spot-check the schedule against the ward's official English + Japanese collection guide before shipping.

Once (1)–(3) are done and committed to Supabase, the app renders that ward correctly with no further code changes.

## Where the data comes from

### Primary sources (per ward)

- **Ward-specific official pages** — every ward publishes an English or bilingual "Trash Sorting Guide" or 「ごみ・資源」ページ. These are authoritative but the format varies wildly (PDF, HTML tables, image maps). Examples:
  - Setagaya: `https://www.city.setagaya.lg.jp/mokuji/kurashi/002/003/index.html`
  - Shinjuku: `https://www.city.shinjuku.lg.jp/kankyo/kankyo01_002001.html`
  - Shibuya: `https://www.city.shibuya.tokyo.jp/kurashi/gomi/`
  - Every other ward has an equivalent under `city.<slug>.lg.jp` or `city.<slug>.tokyo.jp`.

### Aggregators (partial coverage)

- **[5374.jp](https://5374.jp/)** ("ゴミナシ") — civic-tech project publishing structured JSON schedules for many municipalities. Coverage of Tokyo's 23 wards is *uneven* — some are present with up-to-date data, some are stale, some are missing. Always cross-check against the ward's own page before importing.

### What NOT to do

- Do not populate schedules from memory, blog posts, or LLM output. Collection days change (holiday adjustments, ward reforms) and getting them wrong sends users out with trash on the wrong day.
- Do not use JIS municipality codes as `garbage_wards.code` values without also updating every existing `garbage_areas.wardCode` foreign-key reference (currently `setagaya` is a text slug). Mixing the two would create split data.

## Suggested ordering for future seeding

Start with wards that have (a) large foreign-resident populations and (b) usable open data. In rough priority order for this app's audience:

1. **Shinjuku** (`shinjuku`) — largest foreign population; ward publishes bilingual PDFs.
2. **Minato** (`minato`) — large embassy/expat concentration.
3. **Shibuya** (`shibuya`), **Toshima** (`toshima`), **Meguro** (`meguro`) — high foreign density; well-documented schedules.
4. **Setagaya** (`setagaya`) — already seeded; expand from one example area to more chōme.
5. **The remaining 18 wards** — as data and time allow.

## UX follow-up (out of scope for this seed PR)

When a user selects a ward that has no `garbage_areas` rows yet, the district dropdown is empty and the Save button stays disabled. That is technically correct behavior but a dead-end from the user's point of view. A small UI improvement — a banner reading something like *"Schedule data for this ward isn't available in the app yet. We're working on it."* — would explain the state without over-promising. Track that as a follow-up.
