# Supabase security checklist

The app relies on Supabase Row-Level Security (RLS) as its **only** authorization boundary. If any of the checks below are missing or misconfigured, users can read, modify, or delete data belonging to other users — even though the Kotlin code filters by `userId` client-side, that filter is advisory, not a security boundary.

Run each check in the **Supabase SQL Editor** for the project. Every check should return one row (or the shape indicated).

## 1. RLS is enabled on every user-writable table

```sql
select relname, relrowsecurity
from pg_class
where relname in (
  'profiles', 'bills', 'documents',
  'garbage_user_settings',
  'user_procedures', 'user_procedure_steps'
)
order by relname;
```

Every row must show `relrowsecurity = t`. If any is `f`, run:
```sql
alter table "<name>" enable row level security;
```

## 2. Read-only reference tables allow SELECT to authenticated users only

Reference tables the client only reads:

```sql
select relname, relrowsecurity
from pg_class
where relname in (
  'garbage_wards', 'garbage_areas', 'garbage_categories', 'garbage_schedules',
  'procedures', 'procedure_steps'
);
```

RLS must be enabled, and there should be exactly **one policy per table** allowing `SELECT` to `authenticated`, with **no** `INSERT/UPDATE/DELETE` policies. Verify:

```sql
select tablename, policyname, cmd
from pg_policies
where tablename in (
  'garbage_wards', 'garbage_areas', 'garbage_categories', 'garbage_schedules',
  'procedures', 'procedure_steps'
)
order by tablename, cmd;
```

## 3. Per-user tables scope every operation to `auth.uid() = "userId"`

```sql
select tablename, policyname, cmd, qual, with_check
from pg_policies
where tablename in (
  'profiles', 'bills', 'documents',
  'garbage_user_settings',
  'user_procedures'
)
order by tablename, cmd;
```

For each of `select`, `insert`, `update`, `delete`, the policy must include an `auth.uid() = "userId"` predicate (in `qual` for select/update/delete, and in `with_check` for insert/update). Missing predicates mean any authenticated user can access or overwrite any row.

## 4. `user_procedure_steps` scopes through its parent

`user_procedure_steps` has no `userId` column — it must scope via a subquery on `user_procedures`:

```sql
select policyname, cmd, qual, with_check
from pg_policies
where tablename = 'user_procedure_steps';
```

Each policy's `qual`/`with_check` should include something like:

```sql
exists (
  select 1 from "user_procedures" up
  where up."id" = "user_procedure_steps"."userProcedureId"
    and up."userId" = auth.uid()
)
```

## 5. Storage buckets

The app writes to two buckets: `profiles` (avatars) and `Bills` (bill photos). Both must scope by the first path segment being the user's UID.

In the Supabase dashboard → **Storage** → each bucket → **Policies**:

- `profiles` and `Bills` must have `SELECT`, `INSERT`, `UPDATE`, `DELETE` policies restricted so that `auth.uid()::text = (storage.foldername(name))[1]`. That matches how the app names files (`<userId>/avatar.jpg`, `<userId>/<timestamp>.jpg`).
- If either bucket is marked **Public**, anyone with a URL can read any file — acceptable for `publicUrl`-based reads in this app, but confirm it's an intentional decision.

## 6. Auth settings

- Confirm **email confirmation** setting: on/off matches what the signup screen expects. Currently the signup flow shows *"Account created. Please check your email to confirm your address, then log in."* only when `currentUserOrNull()` is null immediately after `signUpWith(Email)` — i.e. it assumes confirmation is enabled. If confirmation is off, the flow works but that message never triggers.
- Confirm the **JWT expiration** for anon sessions is what you want (current anon key expires 2036 per its `exp` claim).

## 7. Client-side reality checks

These are patterns in the Kotlin code where correctness **depends** on RLS being right:

| File | Line pattern | RLS requirement |
|---|---|---|
| `BillViewModel.deleteBill` / `updateBill` | `filter { eq("id", bill.id) }` — no userId filter | RLS must enforce `userId = auth.uid()` on `bills` |
| `DocumentViewModel.deleteDocument` / `updateDocument` | `filter { eq("id", doc.id) }` — no userId filter | RLS must enforce `userId = auth.uid()` on `documents` |
| `GarbageRepository.saveUserSettings` | `upsert(settings)` | RLS must reject rows where `userId != auth.uid()` |
| `ProcedureRepository.setStepCompleted` | Filter is by `userProcedureId` only | RLS on `user_procedure_steps` must join to `user_procedures` and check `auth.uid()` |

If any of these tables lack ownership-checking policies, an authenticated user can tamper with rows they don't own by guessing their ID.

## When to re-run

- After any new table is added to Supabase.
- After any change to policies in the dashboard.
- When rotating auth or storage settings.

None of these checks are enforced by CI — RLS lives outside the code repository.
